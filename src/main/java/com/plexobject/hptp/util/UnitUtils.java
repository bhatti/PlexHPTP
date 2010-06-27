package com.plexobject.hptp.util;

public class UnitUtils {
    public static final int K = 1024;
    public static final int M = 1024 * K;
    public static final long G = 1024 * M;
    public static final long ONE_MILLI_SEC = 1000000L;
    public static final long ONE_SEC = ONE_MILLI_SEC * 1000;
    public static final long ONE_MINUTE = ONE_SEC * 60;
    public static final long ONE_HOUR = ONE_MINUTE * 60;
    public static final long ONE_DAY = ONE_HOUR * 24;

    public static String formatFileSize(long size) {
        return formatFileSize(size * 1.0);
    }

    public static String formatFileSize(double size) {
        if (size < 0) {
            return "-";
        } else if (size > G) {
            return String.format("%.2fG", size / G);
        } else if (size > M) {
            return String.format("%.2fM", size / M);
        } else if (size > K) {
            return String.format("%.2fK", size / K);
        } else {
            return String.format("%.2fB", size);
        }
    }

    public static String formatMilliTime(long time) {
        return formatMilliTime(time * 1.0);
    }

    public static String formatMilliTime(double time) {
        return formatNanoTime(time * ONE_MILLI_SEC);
    }

    public static String formatNanoTime(long time) {
        return formatNanoTime(time * 1.0);
    }

    public static String formatNanoTime(double time) {
        if (time < 0) {
            return "-";
        } else if (time > ONE_DAY) {
            return String.format("%.2f dys", time / ONE_DAY);
        } else if (time > ONE_HOUR) {
            return String.format("%.2f hrs", time / ONE_HOUR);
        } else if (time > ONE_MINUTE) {
            return String.format("%.2f mns", time / ONE_MINUTE);
        } else if (time > ONE_SEC) {
            return String.format("%.2f scs", time / ONE_SEC);
        } else if (time > ONE_MILLI_SEC) {
            return String.format("%.2f ms", time / ONE_MILLI_SEC);
        } else {
            return String.format("%.2f nn", time);
        }
    }
}
