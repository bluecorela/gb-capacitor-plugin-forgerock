package com.plugins.forgerockbridge;

import com.getcapacitor.PluginCall;

public class ErrorHandler {
    public enum OTPErrorCode {
        UNKNOWN_ERROR("FRE000"),
        MISSING_JOURNEY("FRE024"),
        AUTHENTICATE_FAILED("FRE025"),
        NO_ACCOUNTS_REGISTERED("FRE026"),
        NO_OTP_REGISTERED("FRE027"),
        WITHOUT_INITIALIZED_SHARED("FRE028"),
        DELETE_OTP_FAILED("FRE029"),
        CALLBACK_FAILED("FRE030"),
        REGISTER_OTP_FAILED("FRE031");

        public final String code;
        OTPErrorCode(String code) { this.code = code; }
    }


    public static void reject(PluginCall call, OTPErrorCode code) {
      call.reject("", code.code);
    }

}
