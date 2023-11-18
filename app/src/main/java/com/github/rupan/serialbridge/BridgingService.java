/*
Copyright 2023 Michael Mohr <akihana@gmail.com>

This file is part of Serial Bridge.

This program is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along
with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.github.rupan.serialbridge;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/*
Future thoughts:

Maybe use https://github.com/jjenkov/java-nio-server instead?
A call to the interrupt() method of your Thread object can stop even blocking I/O

https://www.geeksforgeeks.org/services-in-android-with-example/#
https://stackoverflow.com/questions/11099305/why-does-my-android-service-block-the-ui
https://developer.android.com/guide/components/services

Can use Thread.isAlive() to check whether it is running
 */

public class BridgingService extends Service {
    InetAddress mBindAddress = null;
    UsbDevice mUsbDevice = null;

    Thread mServiceThread = new Thread() {
        public void run() {
            try ( ServerSocketChannel serverSocketChannel = ServerSocketChannel.open() ) {
                InetSocketAddress xxyy = new InetSocketAddress(mBindAddress, (int)9999);
                serverSocketChannel.socket().bind(xxyy);

                while (true) {
                    SocketChannel _socketChannel = serverSocketChannel.accept();
                }
            } catch (IOException ioe) {
                // TODO: handle this
            }
        }
    };

    /*
    @Override
    public void onCreate() {
        // The service is being created.
        // This function initializes state for the service kind of like a constructor.
    }
    */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService().
        // Multiple requests to start the service result in multiple corresponding calls
        // to the service's onStartCommand().

        super.onStartCommand(intent, flags, startId);
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        mBindAddress = intent.getParcelableExtra("com.github.rupan.serialbridge.BindAddr", InetAddress.class);
        mUsbDevice = intent.getParcelableExtra("com.github.rupan.serialbridge.UsbDevice", UsbDevice.class);
        mServiceThread.start();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed.

        super.onDestroy();
        Toast.makeText(this, "service stopping", Toast.LENGTH_SHORT).show();
        mServiceThread.interrupt();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        // We don't provide binding, so return null

        return null;
    }

    /*
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return allowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    */
}
