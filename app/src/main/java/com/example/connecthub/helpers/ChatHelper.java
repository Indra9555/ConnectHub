package com.example.connecthub.helpers;

public class ChatHelper {

    public static String getChatId(String uid1, String uid2) {

        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }

    }

}