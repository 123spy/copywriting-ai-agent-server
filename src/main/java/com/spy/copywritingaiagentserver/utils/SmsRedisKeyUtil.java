package com.spy.server.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class SmsRedisKeyUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String SMS_LOGIN_PREFIX = "dianping:sms:login:";
    private static final String SMS_COOLDOWN_PREFIX = "dianping:sms:cooldown:";
    private static final String SMS_PHONE_LIMIT_PREFIX = "dianping:sms:limit:phone:";

    private SmsRedisKeyUtil() {
    }

    public static String loginCodeKey(String phone) {
        return SMS_LOGIN_PREFIX + phone;
    }

    public static String sendCooldownKey(String phone) {
        return SMS_COOLDOWN_PREFIX + phone;
    }

    public static String phoneDailyLimitKey(String phone, LocalDate date) {
        return SMS_PHONE_LIMIT_PREFIX + date.format(DATE_FORMATTER) + ":" + phone;
    }
}
