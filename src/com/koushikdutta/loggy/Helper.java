package com.koushikdutta.loggy;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.security.MessageDigest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;

public class Helper {
    static public boolean isJavaScriptNullOrEmpty(String s) {
        return s == null || s.equals("") || s.equals("null");
    }
    
    static public String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
        }
        catch (Exception e) {
            return null;
        }
    }
    
    @SuppressLint("NewApi")
    static public String getSafeDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();
        if (deviceId == null) {
            String wifiInterface = SystemProperties.get("wifi.interface");
            try {
                if (Build.VERSION.SDK_INT < 9)
                    throw new Exception();
                String wifiMac = new String(NetworkInterface.getByName(wifiInterface).getHardwareAddress());
                deviceId = wifiMac;
            }
            catch (Exception e) {
                deviceId = "000000000000";
            }
        }
        deviceId += context.getPackageName();
        String ret = digest(deviceId);
        return ret;
    }

    static public void showAlertDialog(Context context, int stringResource)
    {
        showAlertDialog(context, null, context.getString(stringResource), null);
    }

    static public void showAlertDialog(Context context, int titleResource, int stringResource)
    {
        showAlertDialog(context, context.getString(titleResource), context.getString(stringResource), null);
    }

    static public void showAlertDialog(Context context, int stringResource, DialogInterface.OnClickListener okCallback)
    {
        showAlertDialog(context, null, context.getString(stringResource), okCallback);
    }
    
    static public void showAlertDialog(Context context, String s)
    {
        showAlertDialog(context, null, s, null);
    }

    static public void showAlertDialog(Context context, String title, String s, DialogInterface.OnClickListener okCallback)
    {
        AlertDialog.Builder builder = new Builder(context);
        builder.setMessage(s);
        if (title != null)
            builder.setTitle(title);
        builder.setPositiveButton(android.R.string.ok, okCallback);
        builder.setCancelable(false);
        builder.create().show();
    }
}
