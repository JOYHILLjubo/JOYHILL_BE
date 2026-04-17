package com.joyhill.demo.common.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    /** 하이픈 제거 후 저장 */
    public static String normalize(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }

    /** 조회 시 하이픈 추가 (01012345678 → 010-1234-5678) */
    public static String format(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        } else if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return phone;
    }
}
