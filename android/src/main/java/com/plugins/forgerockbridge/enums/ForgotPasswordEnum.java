package com.plugins.forgerockbridge.enums;
public class ForgotPasswordEnum {
    public enum IdPath {
        INIT_FORGOT_PASS("INIT_FORGOT_PASS"),
        ANSWER_QUESTION("ANSWER_QUESTION"),
        CHANGE_PASS("CHANGE_PASS");
        public final String code;
        IdPath(String code) { this.code = code; }
    }
}