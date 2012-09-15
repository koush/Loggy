package com.koushikdutta.loggy;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.ClosedCallback;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.server.WebSocket;
import com.koushikdutta.async.http.server.WebSocketCallback;

public class LoggyService extends Service {
    AsyncHttpServer mServer;
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        mServer.stop();
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
                    StringBuilder b = new StringBuilder();
                    b.append(String.format("<html><title>Loggy</title><head><script>view = '/views/%s.jade';</script><script src='/javascripts/jquery-1.8.1.min.js'></script><script src='/javascripts/jade.min.js'></script><script src='/javascripts/render.js'></script></head><body></body></html>", view));
                    response.send(b.toString());
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

    private void view(String regex, final String view) {
        mServer.get(regex, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                StringBuilder b = new StringBuilder();
                b.append(String.format("<html><title>Loggy</title><head><script>view = '/views/%s.jade';</script><script src='/javascripts/jquery-1.8.1.min.js'></script><script src='/javascripts/jade.min.js'></script><script src='/javascripts/render.js'></script></head><body></body></html>", view));
                response.send(b.toString());
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
    
    @Override
    public void onCreate() {
        mServer = new AsyncHttpServer(Settings.getInstance(this).getInt("port", 3000));
        
        view("/", "index");

        view("/logcat", "logcat");

        mServer.websocket("/logcat/stream", new WebSocketCallback() {
            Process process;
            @Override
            public void onConnected(final WebSocket webSocket) {
                new Thread() {
                    public void run() {
                        try {
                            // get the last 100 lines
                            String last = null;
                            String s;
                            process = Runtime.getRuntime().exec(new String[] { "su", "-c", "/system/bin/logcat -t 100" });
                            DataInputStream dis = new DataInputStream(process.getInputStream());
                            while (null != (s = dis.readLine())) {
                                if (s.length() == 0)
                                    continue;
                                last = s;
                                webSocket.send(s);
                            }
                            dis.close();

                            // now get the running log
                            process = Runtime.getRuntime().exec("su -c /system/bin/logcat");
                            boolean skip = true;
                            dis = new DataInputStream(process.getInputStream());
                            while (null != (s = dis.readLine())) {
                                if (s.length() == 0)
                                    continue;
                                if (last.equals(s))
                                    skip = false;
                                if (!skip)
                                    webSocket.send(s);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                }.start();
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                    }
                });
                webSocket.setClosedCallback(new ClosedCallback() {
                    @Override
                    public void onClosed() {
                        try {
                            process.destroy();
                        }
                        catch (Exception e)  {
                            // destroy will cause some angry noise in logcat and throw, since
                            // we are trying to destroy a root process.
                            // the kill succeeds, but it still gets angry.
                            // i assume it succeeds because the various streams are closed.
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
