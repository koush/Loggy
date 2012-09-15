package com.koushikdutta.loggy;

public interface Callback<T> {
    void onResult(T result);
}