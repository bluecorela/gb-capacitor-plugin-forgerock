package com.plugins.forgerockbridge.interceptor;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.Request;
import org.forgerock.android.auth.RequestInterceptor;

public class ForgotPasswordInterceptor implements RequestInterceptor {

    private final String languageCode;

    public ForgotPasswordInterceptor(String languageCode) {
        this.languageCode = languageCode;
    }

    @NonNull
    @Override
    public Request intercept(Request request) {
        return request.newBuilder()
                .addHeader("Accept-Language", languageCode)
                .build();
    }
}