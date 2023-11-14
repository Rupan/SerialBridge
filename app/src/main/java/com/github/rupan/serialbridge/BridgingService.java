package com.github.rupan.serialbridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class BridgingService extends Service {
    // https://www.geeksforgeeks.org/services-in-android-with-example/#
    // https://stackoverflow.com/questions/11099305/why-does-my-android-service-block-the-ui
    Thread mServiceThread = null;

    @Override
    public void onCreate() {
        // Maybe use https://github.com/jjenkov/java-nio-server instead?
        // A call to the interrupt() method of your Thread object can stop even blocking I/O
        mServiceThread = new Thread() {
            public void run() {
                try ( ServerSocketChannel serverSocketChannel = ServerSocketChannel.open() ) {
                    InetSocketAddress xxyy = new InetSocketAddress((InetAddress)null, (int)9999);
                    serverSocketChannel.socket().bind(xxyy);

                    while (true) {
                        SocketChannel _socketChannel = serverSocketChannel.accept();
                    }
                } catch (IOException ioe) {
                    // TODO: handle this
                }
            }
        };
        mServiceThread.start();
    }

    @Override
    public void onDestroy() {
        if(mServiceThread == null) {
            return;
        }
        mServiceThread.interrupt();
        mServiceThread = null;
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY;
//    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
