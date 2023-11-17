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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

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
    private static final String ACTION_USB_PERMISSION = "com.github.rupan.serialbridge.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d("SOME TAG", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Nullable
    private byte[] serialize_ia(@Nullable InetAddress intf_addr) {
        if( intf_addr == null ) {
            Toast.makeText(this,"Null InetAddress", Toast.LENGTH_SHORT).show();
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(intf_addr);
            return bos.toByteArray();
        } catch( IOException ioe ) {
            Toast.makeText(this,"InetAddress serialization failed", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private HashMap<String, InetAddress> get_all_addrs() throws SocketException, NullPointerException {
        HashMap<String, InetAddress> fb = new HashMap<>();
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface intf = e.nextElement();
            Enumeration<InetAddress> i = intf.getInetAddresses();
            while (i.hasMoreElements()) {
                InetAddress intf_addr = i.nextElement();
                String hn = String.format("%s: %s", intf.getName(), intf_addr.getHostAddress());
                fb.put( hn, intf_addr );
            }
        }
        return fb;
    }

    private HashMap<String, UsbDevice> get_all_serial_devs() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = new HashMap<>();
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mUsbReceiver, filter, RECEIVER_NOT_EXPORTED);
        }
        for( UsbSerialDriver driver : UsbSerialProber.getDefaultProber().findAllDrivers(manager) ) {
            UsbDevice device = driver.getDevice();
            manager.requestPermission(device, permissionIntent);
            String desc = String.format("%s (%s)",
                device.getProductName(),
                device.getManufacturerName()
            );
            devices.put( desc, driver.getDevice() );
        }
        return devices;
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
            HashMap<String, InetAddress> address_map = get_all_addrs();
            byte[] sia = serialize_ia( address_map.get("lo: ::1") ); // FIXME
            if( sia != null ) {
                bridgingIntent.putExtra("com.github.rupan.serialbridge.BindAddr", sia);
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
        {
            HashMap<String, UsbDevice> device_map = get_all_serial_devs();
            List<String> addrlist = new ArrayList<>(device_map.size());
            addrlist.addAll(device_map.keySet());
            Collections.sort(addrlist);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, addrlist);
            AppCompatAutoCompleteTextView textView = findViewById(R.id.usb_device_dropdown);
            textView.setAdapter(adapter);
        }
        Button serviceButton = findViewById(R.id.serviceButton);
        serviceButton.setOnClickListener(new View.OnClickListener(){
            private boolean mServiceStarted = false;
            @Override
            public void onClick(View view) {
                if( !mServiceStarted ) {
                    startService( bridgingIntent );
                    serviceButton.setText("Stop service");
                    mServiceStarted = true;
                } else {
                    stopService( bridgingIntent );
                    serviceButton.setText("Start service");
                    mServiceStarted = false;
                }
            }
        });
    }
}
