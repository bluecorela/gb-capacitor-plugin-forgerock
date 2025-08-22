package com.plugins.forgerockbridge;

import com.getcapacitor.PluginCall;

public class ErrorHandler {
    public enum ErrorCode {
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
        ErrorCode(String code) { this.code = code; }
    }


    public static void reject(PluginCall call, ErrorCode code) {
      call.reject("", code.code);
    }

    public static class FRException extends Exception {
      private final ErrorHandler.ErrorCode code;

      public FRException(ErrorHandler.ErrorCode code) {
          super(code.name());
          this.code = code;
      }
      
      public ErrorHandler.ErrorCode getCode() {
          return code;
      }
    }

}
