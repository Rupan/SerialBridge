package com.github.rupan.serialbridge;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class BridgingActivity extends AppCompatActivity {

    private List<String> get_all_addrs() throws SocketException, NullPointerException {
        List<String> fa = new ArrayList<>();
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface intf = e.nextElement();
            Enumeration<InetAddress> i = intf.getInetAddresses();
            while (i.hasMoreElements()) {
                InetAddress intf_addr = i.nextElement();
                fa.add(String.format("%s: %s", intf.getName(), intf_addr.getHostAddress()));
            }
        }
        return fa;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridging);
        // final TextView t = findViewById(R.id.network_parameters_header);
        // t.setText("some static text");
        // t.setText(R.string.user_greeting);
        try {
            List<String> addrlist = get_all_addrs();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, addrlist);
            AppCompatAutoCompleteTextView textView = findViewById(R.id.ip_address_dropdown);
            textView.setAdapter(adapter);
        } catch (SocketException | NullPointerException se) {
            // TODO: log something (maybe a toast?)
        }
        // getApplicationContext() ???
        startService( new Intent( this, BridgingService.class ) );
        stopService( new Intent( this, BridgingService.class ) );
    }
}
