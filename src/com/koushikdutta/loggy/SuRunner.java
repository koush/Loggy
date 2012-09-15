package com.koushikdutta.loggy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.os.Handler;

public class SuRunner {
    HashMap<String, String> mEnvironment = new HashMap<String, String>();
    StringBuilder mCommands = new StringBuilder();
    
    public static boolean hasRoot() {
        File f = new File("/system/bin/su");
        if (f.exists())
            return true;
        f = new File("/system/xbin/su");
        return f.exists();
    }

    public void setEnvironment(String name, String value) {
        mEnvironment.put(name, value);
    }

    public void runSuCommandAsync(final Context context, final SuCommandCallback callback) {
        Handler handler = null;
        try {
            if (callback != null)
                handler = new Handler();
        }
        catch (Exception ex) {
        }

        final Handler finalHandler = handler;

        Thread thread = new Thread() {
            int result = -1;

            @Override
            public void run() {
                try {
                    callback.onStartBackground();
                    Process p = runSuCommandAsync(context);
                    if (p == null) {
                        return;
                    }
                    DataInputStream dis = new DataInputStream(p.getInputStream());
                    String line;
                    while (null != (line = dis.readLine())) {
                        if (callback != null) {
                            if (finalHandler != null) {
                                final String fline = line;
                                finalHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onOutputLine(fline);
                                    }
                                });
                            }
                            else {
                                callback.onOutputLine(line);
                            }
                        }
                    }
                    dis.close();
                    result = p.waitFor();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (callback == null)
                        return;
                    callback.onResultBackground(result);
                    if (finalHandler != null) {
                        finalHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResult(result);
                            }
                        });
                    }
                    else {
                        callback.onResult(result);
                    }
                }
            }
        };
        thread.start();
    }

    public Process runSuCommandAsync(Context context) {
        try {
            Process ret = Runtime.getRuntime().exec("su");
            DataOutputStream fout = new DataOutputStream(ret.getOutputStream());

            for (String key : mEnvironment.keySet()) {
                String value = mEnvironment.get(key);
                if (value == null)
                    continue;
                fout.writeBytes(String.format("export %s='%s'\n", key, value));
            }
            fout.writeBytes(mCommands.toString());
            fout.flush();
            fout.close();
            return ret;
        }
        catch (Exception ex) {
            return null;
        }
    }

    public Process runSuCommand(Context context) {
        try {
            return runSuCommandAsync(context);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int runSuCommandForResult(Context context) {
        try {
            return runSuCommandAsync(context).waitFor();
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void addCommand(String command) {
        mCommands.append(command);
        mCommands.append('\n');
    }
}