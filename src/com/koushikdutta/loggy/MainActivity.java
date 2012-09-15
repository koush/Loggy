package com.koushikdutta.loggy;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
    ImageView mWifiIcon;
    TextView mLoggyStatus;
    TextView mLoggyAt;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mWifiIcon = (ImageView)findViewById(R.id.wifi_icon);
        mLoggyStatus = (TextView)findViewById(R.id.loggy_status);
        mLoggyAt = (TextView)findViewById(R.id.loggy_at);
        refreshService();
    }
    
    boolean mDestroyed = false;
    @Override
    protected void onDestroy() {
        mDestroyed = true;
        super.onDestroy();
    };
    private void refreshService() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean _enabled = false;
        for (RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (LoggyService.class.getName().equals(service.service.getClassName())) {
                _enabled = true;
                break;
            }
        }
        final boolean enabled = _enabled;
        AnimatedView.setOnClickListener(mWifiIcon, new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enabled) {
                    stopService(new Intent(MainActivity.this, LoggyService.class));
                }
                else {
                    if (!SuRunner.hasRoot()) {
                        Helper.showAlertDialog(MainActivity.this, R.string.superuser, R.string.superuser_not_detected);
                        return;
                    }
                    // force the su dialog
                    new Thread() {
                        public void run() {
                            SuRunner runner = new SuRunner();
                            runner.addCommand("echo test");
                            runner.runSuCommand(MainActivity.this);
                        };
                    }.start();
                    startService(new Intent(MainActivity.this, LoggyService.class));
                }
            }
        });
        
        if (enabled) {
            mWifiIcon.setImageResource(R.drawable.wifi_on);
            String wifiInterface = SystemProperties.get("wifi.interface");
            if (wifiInterface != null) {
                try {
                    NetworkInterface iface = NetworkInterface.getByName(wifiInterface);
                    if (iface != null) {
                        wifiInterface = null;
                        Enumeration<InetAddress> iter = iface.getInetAddresses();
                        while (iter.hasMoreElements()) {
                            InetAddress addr = iter.nextElement();
                            if (!(addr instanceof Inet4Address))
                                continue;
                            wifiInterface = addr.getHostAddress();
                            break;
                        }
                    }
                }
                catch (Exception e) {
                }
            }
            if (wifiInterface == null) {
                try {
                    int ip = ((WifiManager)(getSystemService(WIFI_SERVICE))).getConnectionInfo().getIpAddress();
                    InetAddress addr = InetAddress.getByAddress(new byte[] {
                                                                    (byte)(ip >>> 24),
                                                                    (byte)(ip >>> 16),
                                                                    (byte)(ip >>> 8),
                                                                    (byte)ip});
                    wifiInterface = addr.getHostAddress();
                }
                catch (Exception e) {
                }
            }
            if (wifiInterface != null) {
                mLoggyStatus.setText(R.string.loggy_on);
                mLoggyAt.setText("http://" + wifiInterface + ":" + Settings.getInstance(MainActivity.this).getInt("port", 3000) + "/");
            }
            else {
                mLoggyAt.setText(" ");
                mLoggyStatus.setText(R.string.loggy_what);
            }
        }
        else {
            mWifiIcon.setImageResource(R.drawable.wifi_off);
            mLoggyStatus.setText(R.string.loggy_off);
            mLoggyAt.setText(" ");
        }
        
        
        if (!mDestroyed) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshService();
                }
            }, 500);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
