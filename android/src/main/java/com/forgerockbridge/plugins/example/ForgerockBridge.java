package com.forgerockbridge.plugins.example;

import android.util.Log;

public class ForgerockBridge {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
