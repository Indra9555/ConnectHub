package com.example.connecthub.helpers;

public class TimeUtils {

    public static String getTimeAgo(long time) {

        long now = System.currentTimeMillis();

        long diff = now - time;

        long seconds = diff / 1000;

        long minutes = seconds / 60;

        long hours = minutes / 60;

        long days = hours / 24;

        if (seconds < 60) {

            return "Just now";

        }

        if (minutes < 60) {

            return minutes + " min ago";

        }

        if (hours < 24) {

            return hours + " hour ago";

        }

        if (days == 1) {

            return "Yesterday";

        }

        return days + " days ago";

    }

}