package com.koushikdutta.loggy;

public abstract class SuCommandCallback implements Callback<Integer> {
    void onStartBackground() {};
    void onOutputLine(String line) {};
    void onResultBackground(int result) {};
}