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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class BridgingActivity extends AppCompatActivity {

    private HashMap<String, byte[]> get_all_addrs() throws SocketException, NullPointerException {
        HashMap<String, byte[]> fb = new HashMap<>();
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface intf = e.nextElement();
            Enumeration<InetAddress> i = intf.getInetAddresses();
            while (i.hasMoreElements()) {
                InetAddress intf_addr = i.nextElement();
                String hn = String.format("%s: %s", intf.getName(), intf_addr.getHostAddress());
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(intf_addr);
                    fb.put( hn, bos.toByteArray() );
                } catch( IOException ioe ) {
                    Toast.makeText(this,
                            "InetAddress serialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return fb;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridging);
        // final TextView t = findViewById(R.id.network_parameters_header);
        // t.setText("some static text");
        // t.setText(R.string.user_greeting);
        Intent bridgingIntent = new Intent( this, BridgingService.class );
        try {
            HashMap<String, byte[]> address_map = get_all_addrs();
            byte[] ipv6_loopback = address_map.get("lo: ::1"); // FIXME
            if( ipv6_loopback != null ) {
                bridgingIntent.putExtra("com.github.rupan.serialbridge.BindAddr", ipv6_loopback);
            }
            List<String> addrlist = new ArrayList<>(address_map.size());
            addrlist.addAll(address_map.keySet());
            Collections.sort(addrlist);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, addrlist);
            AppCompatAutoCompleteTextView textView = findViewById(R.id.ip_address_dropdown);
            textView.setAdapter(adapter);
        } catch (SocketException | NullPointerException se) {
            // TODO: log something (maybe a toast?)
        }
        // getApplicationContext() ???
        startService( bridgingIntent );
        stopService( bridgingIntent );
    }
}
