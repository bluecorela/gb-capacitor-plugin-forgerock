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
        DELETE_OTP_MECHANISM_FAILED("FRE030"),
        CALLBACK_FAILED("FRE031"),
        REGISTER_OTP_FAILED("FRE032"),
        GETTING_USER_INFO("FRE033"),
        HTTP_REQUEST_ERROR("FRE034");

        public final String code;
        OTPErrorCode(String code) { this.code = code; }
    }


    public static void reject(PluginCall call, OTPErrorCode code) {
      call.reject("", code.code);
    }

    public static class OTPException extends Exception {
      private final ErrorHandler.OTPErrorCode code;

      public OTPException(ErrorHandler.OTPErrorCode code) {
          super(code.name());
          this.code = code;
      }
      
      public ErrorHandler.OTPErrorCode getCode() {
          return code;
      }
    }

}
