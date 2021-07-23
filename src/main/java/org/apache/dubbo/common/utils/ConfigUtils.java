package org.apache.dubbo.common.utils;

public class ConfigUtils {
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return StringUtils.isEmpty(value)
                       || "false".equalsIgnoreCase(value)
                       || "0".equalsIgnoreCase(value)
                       || "null".equalsIgnoreCase(value)
                       || "N/A".equalsIgnoreCase(value);
    }
}
