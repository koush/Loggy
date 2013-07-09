package com.koushikdutta.loggy;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServer.WebSocketRequestCallback;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class LoggyService extends Service {
    AsyncHttpServer mServer;
    
    @Override
    public void onDestroy() {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(LOGGY_NOTIFICATION);
        super.onDestroy();
        mServer.stop();
        AsyncServer.getDefault().stop();
    }
    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    
    private void viewDir(String regex, final File directory, final String view) {
        mServer.get(regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getMatcher().replaceAll("");
                File file = new File(directory, path);
                
                if (!file.exists()) {
                    response.responseCode(404);
                    response.send("Not Found");
                    return;
                }

                if (file.isDirectory()) {
                    try {
                        if (mLayout == null)
                            mLayout = readFully(AsyncHttpServer.getAssetStream(LoggyService.this, "layout.html"));
                    }
                    catch (IOException e) {
                        response.send("internal error");
                        return;
                    }
                    response.send(String.format(mLayout, view));
                    return;
                }
                
                try {
                    response.getHeaders().getHeaders().add("Content-Type", AsyncHttpServer.getContentType(path));
                    FileInputStream is = new FileInputStream(file);
                    response.responseCode(200);
                    Util.pump(is, response, new CompletedCallback() {
                        @Override
                        public void onCompleted(Exception ex) {
                            response.end();
                        }
                    });
                }
                catch (Exception ex) {
                    response.responseCode(404);
                    response.end();
                    return;
                }
            }
        });
    }

    static String readFully(InputStream input) throws IOException {
        byte[] buffer = new byte[input.available()];
        DataInputStream dinput = new DataInputStream(input);
        dinput.readFully(buffer);
        dinput.close();
        return new String(buffer);
    }
    
    private String mLayout;
    private void view(String regex, final String view) {
        mServer.get(regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    if (mLayout == null)
                        mLayout = readFully(AsyncHttpServer.getAssetStream(LoggyService.this, "layout.html"));
                }
                catch (IOException e) {
                    response.send("internal error");
                    return;
                }
                response.send(String.format(mLayout, view));
            }
        });
    }
    
    JSONObject jsonFile(File f) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("name", f.getName());
        if (f.isFile())
            j.put("size", f.length());
        j.put("path", f.getAbsolutePath());
        return j;
    }
    
    public void directory(String regex, final File directory) {
        Assert.assertTrue(directory.isDirectory());
        mServer.addAction("GET", regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                String path = request.getMatcher().replaceAll("");
                File file = new File(directory, path);
                
                if (!file.exists()) {
                    response.responseCode(404);
                    response.send("Not Found");
                    return;
                }
                
                if (file.isDirectory()) {
                    ArrayList<File> dirs = new ArrayList<File>();
                    ArrayList<File> files = new ArrayList<File>();
                    for (File f: file.listFiles()) {
                        if (f.isDirectory())
                            dirs.add(f);
                        else
                            files.add(f);
                    }
                    
                    Comparator<File> c = new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    };
                    
                    Collections.sort(dirs, c);
                    Collections.sort(files, c);
                    
                    JSONArray d = new JSONArray();
                    JSONArray f = new JSONArray();
                    for (File ff: files) {
                        try {
                            f.put(jsonFile(ff));
                        }
                        catch (JSONException e) {
                        }
                    }
                    for (File dd: dirs) {
                        try {
                            d.put(jsonFile(dd));
                        }
                        catch (JSONException e) {
                        }
                    }
                    
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("files", f);
                        ret.put("dirs", d);
                    }
                    catch (Exception ex) {
                    }
                    response.send(ret);
                    
                    return;
                }

                if (!file.isFile()) {
                    response.responseCode(404);
                    response.end();
                    return;
                }
                try {
                    FileInputStream is = new FileInputStream(file);
                    response.responseCode(200);
                    Util.pump(is, response, new CompletedCallback() {
                        @Override
                        public void onCompleted(Exception ex) {
                            response.end();
                        }
                    });
                }
                catch (Exception ex) {
                    response.responseCode(404);
                    response.end();
                    return;
                }
            }
        });
    }
    
    private static final String LOGTAG = "Loggy";
    private static final int LOGGY_NOTIFICATION = 3000;
    @Override
    public void onCreate() {
        mServer = new AsyncHttpServer();
        mServer.listen(Settings.getInstance(this).getInt("port", 3000));
        
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification n = new Notification(R.drawable.ic_stat_running, getString(R.string.loggy_on), 0);
        n.flags |= Notification.FLAG_ONGOING_EVENT;
        Intent i = new Intent();
        i.setClass(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, Intent.FLAG_ACTIVITY_NEW_TASK);
        n.setLatestEventInfo(this, getString(R.string.loggy_on), null, pi);
        nm.notify(LOGGY_NOTIFICATION, n);
        
        view("/", "index");

        view("/logcat", "logcat");
        view("/camera", "camera");
        
        mServer.websocket("/camera/stream", new WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, RequestHeaders headers) {
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    public void onReceive(android.content.Context context, Intent intent) {
                        try {
                            final JSONObject result = new JSONObject();
                            String image = intent.getStringExtra("image");
                            image = image.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
                            result.put("image", image);
                            Log.i(LOGTAG, image);
                            new Thread() {
                                public void run() {
                                    webSocket.send(result.toString());
                                };
                            }.start();
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    };
                };
                
                Intent intent = new Intent(MainActivity.CAMERA_INTENT);
                intent.setClass(LoggyService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                IntentFilter filter = new IntentFilter(MainActivity.CAMERA_INTENT);
                registerReceiver(receiver, filter);
                
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        unregisterReceiver(receiver);
                    }
                });
            }
        });

        mServer.websocket("/logcat/stream", new WebSocketRequestCallback() {
            Process process;
            Process kmsgProcess;
            @Override
            public void onConnected(final WebSocket webSocket, RequestHeaders headers) {
                new Thread() {
                    boolean skip = true;
                    public void run() {
                        try {
                            // get the last 100 lines
                            String last = null;
                            final long start = System.currentTimeMillis();
                            String s;
                            SuRunner runner = new SuRunner();
                            runner.addCommand("/system/bin/logcat -t 500 -b radio -b events -b system -b main");
                            process = runner.runSuCommand(LoggyService.this);
                            DataInputStream dis = new DataInputStream(process.getInputStream());
                            while (null != (s = dis.readLine())) {
                                if (s.length() == 0)
                                    continue;
                                last = s;
                                final String line = s;
                                AsyncServer.getDefault().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        webSocket.send(line);
                                    }
                                });
                            }
                            dis.close();
                            
                            // now get the running log
                            runner = new SuRunner();
                            runner.addCommand("/system/bin/logcat -b radio -b events -b system -b main");
                            process = runner.runSuCommand(LoggyService.this);
                            dis = new DataInputStream(process.getInputStream());

                            // also grab kmsg
                            new Thread() {
                                public void run() {
                                    try {
                                        SuRunner krunner = new SuRunner();
                                        krunner.addCommand("cat /proc/kmsg");
                                        kmsgProcess = krunner.runSuCommand(LoggyService.this);
                                        DataInputStream dis = new DataInputStream(kmsgProcess.getInputStream());
                                        String s;
                                        while (null != (s = dis.readLine())) {
                                            if (s.length() == 0)
                                                continue;
                                            if (!skip) {
                                                final String line = s;
                                                AsyncServer.getDefault().post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        webSocket.send("I/kmsg(0): " + line);
                                                    }
                                                });
                                            }
                                        }
                                        Log.i(LOGTAG, "kmsg exited.");
                                        process.destroy();
                                    }
                                    catch (Exception ex) {
                                    }
                                };
                            }.start();

                            while (null != (s = dis.readLine())) {
                                if (s.length() == 0)
                                    continue;
                                if (last.equals(s))
                                    skip = false;
                                if (!skip) {
                                    final String line = s;
                                    AsyncServer.getDefault().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            webSocket.send(line);
                                        }
                                    });
                                }
                            }
                            Log.i(LOGTAG, "logcat exited.");
                            kmsgProcess.destroy();
                        }
                        catch (Exception e) {
                        }
                    };
                }.start();
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                    }
                });
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            kmsgProcess.destroy();
                        }
                        catch (Exception e)  {
                            // destroy will cause some angry noise in logcat and throw, since
                            // we are trying to destroy a root process.
                            // the kill succeeds, but it still gets angry.
                            // i assume it succeeds because the various streams are closed.
                        }
                        try {
                            process.destroy();
                        }
                        catch (Exception e)  {
                        }
                    }
                });
            }
        });

        viewDir("/sdcard", Environment.getExternalStorageDirectory(), "sdcard");
        viewDir("/sdcard/.*?", Environment.getExternalStorageDirectory(), "sdcard");
        directory("/json/sdcard/.*?", Environment.getExternalStorageDirectory());

        mServer.directory(this, "/.*?", "site/");
        
        super.onCreate();
    }
}
