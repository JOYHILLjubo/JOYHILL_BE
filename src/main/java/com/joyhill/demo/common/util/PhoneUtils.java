package com.joyhill.demo.common.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalize(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}
