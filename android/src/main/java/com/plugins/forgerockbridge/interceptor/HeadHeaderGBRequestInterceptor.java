package com.plugins.forgerockbridge.interceptor;

import android.util.Log;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.Request;
import org.forgerock.android.auth.RequestInterceptor;

public class HeadHeaderGBRequestInterceptor implements RequestInterceptor {

    private final String languageCode;

    public HeadHeaderGBRequestInterceptor(String languageCode) {
        this.languageCode = languageCode;
    }

    @NonNull
    @Override
    public Request intercept(Request request) {
        Log.e("TAG", "[ForgotPasswordHandler: startForgotPasswordJourney]: Error in get the username" + request);
        return request.newBuilder()
                .addHeader("Accept-Language", languageCode)
                .addHeader("ForceAuth", "true")
                .build();
    }
}