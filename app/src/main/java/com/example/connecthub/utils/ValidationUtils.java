package com.example.connecthub.utils;

import android.util.Patterns;

public class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    /** Usernames: 3-20 chars, letters / digits / underscore / dot. */
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9_.]{3,20}");
    }
}
