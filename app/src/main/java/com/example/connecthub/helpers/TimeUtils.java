package com.example.connecthub.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    private TimeUtils() {
    }

    /** "Just now", "5 mins ago", "1 hour ago", "Yesterday", "3 days ago", "2 weeks ago", "12 Mar 2026". */
    public static String getTimeAgo(long time) {

        if (time <= 0) {
            return "";
        }

        long diff = Math.max(0, System.currentTimeMillis() - time);

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        }

        if (minutes < 60) {
            return minutes + (minutes == 1 ? " min ago" : " mins ago");
        }

        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        if (days == 1) {
            return "Yesterday";
        }

        if (days < 7) {
            return days + " days ago";
        }

        if (days < 30) {
            long weeks = days / 7;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        }

        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                .format(new Date(time));
    }

    /** Text shown after "Last seen ": "just now", "5 mins ago", "yesterday", ... */
    public static String getLastSeen(long time) {

        if (time <= 0) {
            return "recently";
        }

        long diff = Math.max(0, System.currentTimeMillis() - time);

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "just now";
        }

        if (minutes < 60) {
            return minutes + (minutes == 1 ? " min ago" : " mins ago");
        }

        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        if (days == 1) {
            return "yesterday";
        }

        if (days < 7) {
            return days + " days ago";
        }

        return new SimpleDateFormat("d MMM", Locale.getDefault())
                .format(new Date(time));
    }

    /** Clock time for a chat bubble, e.g. "10:21 PM" (follows the device locale). */
    public static String formatClock(long time) {
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                .format(new Date(time));
    }
}
