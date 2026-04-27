package com.planotech.plano.auth;

public class SecurityConstants {
    private SecurityConstants() {
    }

    public static final String[] PUBLIC_URLS = {
            "/user/login",
            "/user/signup",
            "/user/register",
            "/user/verify-email",
            "/user/resend-otp",
            "/user/refresh",
            "/user/forgot/password/request",
            "/user/auth/reset-password",
            "/public/events/**",
            "/file/upload",
            "/payment/order/**",
            "/test"
    };
}

