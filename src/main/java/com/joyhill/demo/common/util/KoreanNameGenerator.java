package com.joyhill.demo.common.util;

public final class KoreanNameGenerator {

    private KoreanNameGenerator() {
    }

    public static String villageName(String fullName) {
        String suffixTarget = lastTwoOrOne(fullName);
        char lastChar = suffixTarget.charAt(suffixTarget.length() - 1);
        return suffixTarget + (hasBatchim(lastChar) ? "이네" : "네");
    }

    public static String famName(String fullName) {
        return lastTwoOrOne(fullName) + "팸";
    }

    private static String lastTwoOrOne(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.length() >= 2 ? name.substring(name.length() - 2) : name;
    }

    private static boolean hasBatchim(char ch) {
        int base = ch - 0xAC00;
        if (base < 0 || base > 11171) {
            return false;
        }
        return base % 28 != 0;
    }
}
