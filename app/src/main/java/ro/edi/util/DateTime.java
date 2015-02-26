/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.edi.util;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Time;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for parsing a date/time.
 *
 * @author Eduard Scarlat
 */
public final class DateTime {
    /*
     * ANSI C's asctime() format
     *
     * Wdy Mon DD HH:MM:SS YYYY
     *
     * with following variations:
     *
     * Wdy Mon (SP)D HH:MM:SS YYYY
     *
     * Wdy Mon DD HH:MM:SS YYYY GMT
     *
     * HH can be H if the first digit is zero. Mon can be the full name of the month.
     */
    public static final int FORMAT_ANSIC = 0;

    /*
     * ISO 8601
     *
     * YYYY-MM-DDTHH:MM:SS.SSSZ (.SSSZ is optional)
     *
     * RFC 3339
     *
     * YYYY-MM-DDTHH:MM:SS+HHMM
     *
     * e.g. 2010-04-30T06:09:55.709Z 2010-12-22T17:50:38+0000
     */
    public static final int FORMAT_ISO8601 = 1;

    /*
     * RFC 822, updated by RFC 1123
     *
     * Wdy, DD Mon YYYY HH:MM:SS GMT
     *
     * RFC 850, obsoleted by RFC 1036
     *
     * Weekday, DD-Mon-YY HH:MM:SS GMT
     *
     * with following variations:
     *
     * Wdy, DD-Mon-YYYY HH:MM:SS GMT
     *
     * Wdy, (SP)D Mon YYYY HH:MM:SS GMT
     *
     * Wdy, DD Mon YYYY HH:MM:SS GMT
     *
     * Wdy, DD-Mon-YY HH:MM:SS GMT
     *
     * Wdy, DD Mon YYYY HH:MM:SS -HHMM
     *
     * Wdy, DD Mon YYYY HH:MM:SS
     *
     * HH can be H if the first digit is zero. Mon can be the full name of the month.
     */
    public static final int FORMAT_RFC = 2;

    private static final String DATETIME_ANSIC_REGEXP = "[ ]([A-Za-z]{3,9})[ ]+([0-9]{1,2})[ ]"
            + "([0-9]{1,2}:[0-5][0-9]:[0-5][0-9])[ ]([0-9]{2,4})";
    // 2[0-3]|[0-1][0-9]
    private static final String DATETIME_ISO_REGEXP = "(-?(?:[1-9][0-9]*)?[0-9]{4})[-](1[0-2]|0[1-9])[-](3[0-1]|0[1-9]|[1-2][0-9])[T]"
            + "([0-9]{1,2}:[0-5][0-9]:[0-5][0-9])" + "(\\.[0-9]+)?(Z|[+-](?:2[ 0-3]|[0-1][0-9]):?[0-5][0-9])?";
    private static final String DATETIME_RFC_REGEXP = "([0-9]{1,2})[- ]([A-Za-z]{3,9})[- ]([0-9]{2,4})[ ]"
            + "([0-9]{1,2}:[0-5][0-9]:[0-5][0-9])";
    private static final String DATETIME_DATE_ONLY_ISO_REGEXP = "(-?(?:[1-9][0-9]*)?[0-9]{4})[-](1[0-2]|0[1-9])[-](3[0-1]|0[1-9]|[1-2][0-9])";

    /**
     * The compiled version of the date/time regular expressions.
     */
    private static final Pattern DATETIME_ANSIC_PATTERN = Pattern.compile(DATETIME_ANSIC_REGEXP);
    private static final Pattern DATETIME_ISO_PATTERN = Pattern.compile(DATETIME_ISO_REGEXP);
    private static final Pattern DATETIME_RFC_PATTERN = Pattern.compile(DATETIME_RFC_REGEXP);
    private static final Pattern DATETIME_DATE_ONLY_PATTERN = Pattern.compile(DATETIME_DATE_ONLY_ISO_REGEXP);

    private static class TimeOfDay {
        TimeOfDay(int h, int m, int s) {
            this.hour = h;
            this.minute = m;
            this.second = s;
        }

        int hour;
        int minute;
        int second;
    }

    /**
     * Parse a text that represents a date in one of these formats:
     * <p/>
     * ANSI C's asctime() format
     * <p/>
     * ISO 8601
     * <p/>
     * RFC 822, updated by RFC 1123
     * <p/>
     * RFC 850, obsoleted by RFC 1036
     *
     * @param format see FORMAT_ constants in DateTime
     * @return time in milliseconds
     * @throws IllegalArgumentException
     */
    public static long parse(String timeString, int format) throws IllegalArgumentException {
        int date;
        int month;
        int year;
        TimeOfDay timeOfDay;

        switch (format) {
            case FORMAT_ANSIC:
                final Matcher ansicMatcher = DATETIME_ANSIC_PATTERN.matcher(timeString);
                if (ansicMatcher.find()) {
                    month = getMonth(ansicMatcher.group(1));
                    date = getDate(ansicMatcher.group(2));
                    timeOfDay = getTime(ansicMatcher.group(3));
                    year = getYear(ansicMatcher.group(4));
                } else {
                    throw new IllegalArgumentException();
                }
                break;
            case FORMAT_ISO8601:
                final Matcher isoMatcher = DATETIME_ISO_PATTERN.matcher(timeString);
                if (isoMatcher.find()) {
                    year = getYear(isoMatcher.group(1));
                    month = getNumericalMonth(isoMatcher.group(2));
                    date = getDate(isoMatcher.group(3));
                    timeOfDay = getTime(isoMatcher.group(4));
                } else {
                    throw new IllegalArgumentException();
                }
                break;
            case FORMAT_RFC:
                final Matcher rfcMatcher = DATETIME_RFC_PATTERN.matcher(timeString);
                if (rfcMatcher.find()) {
                    date = getDate(rfcMatcher.group(1));
                    month = getMonth(rfcMatcher.group(2));
                    year = getYear(rfcMatcher.group(3));
                    timeOfDay = getTime(rfcMatcher.group(4));
                } else {
                    throw new IllegalArgumentException();
                }
                break;
            default:
                throw new IllegalArgumentException();
        }

        // TODO Y2038 BUG!
        if (year >= 2038) {
            year = 2038;
            month = Calendar.JANUARY;
            date = 1;
        }

        final Time time = new Time(Time.TIMEZONE_UTC);
        time.set(timeOfDay.second, timeOfDay.minute, timeOfDay.hour, date, month, year);
        return time.toMillis(false /* use isDst */);
    }

    private static int getDate(String dateString) {
        if (dateString.length() == 2) {
            return (dateString.charAt(0) - '0') * 10 + (dateString.charAt(1) - '0');
        }

        return (dateString.charAt(0) - '0');
    }

    /*
     * jan = 9 + 0 + 13 = 22
     *
     * feb = 5 + 4 + 1 = 10
     *
     * mar = 12 + 0 + 17 = 29
     *
     * apr = 0 + 15 + 17 = 32
     *
     * may = 12 + 0 + 24 = 36
     *
     * jun = 9 + 20 + 13 = 42
     *
     * jul = 9 + 20 + 11 = 40
     *
     * aug = 0 + 20 + 6 = 26
     *
     * sep = 18 + 4 + 15 = 37
     *
     * oct = 14 + 2 + 19 = 35
     *
     * nov = 13 + 14 + 21 = 48
     *
     * dec = 3 + 4 + 2 = 9
     */
    private static int getMonth(String monthString) {
        final int hash = Character.toLowerCase(monthString.charAt(0)) + Character.toLowerCase(monthString.charAt(1))
                + Character.toLowerCase(monthString.charAt(2)) - 3 * 'a';

        switch (hash) {
            case 22:
                return Calendar.JANUARY;
            case 10:
                return Calendar.FEBRUARY;
            case 29:
                return Calendar.MARCH;
            case 32:
                return Calendar.APRIL;
            case 36:
                return Calendar.MAY;
            case 42:
                return Calendar.JUNE;
            case 40:
                return Calendar.JULY;
            case 26:
                return Calendar.AUGUST;
            case 37:
                return Calendar.SEPTEMBER;
            case 35:
                return Calendar.OCTOBER;
            case 48:
                return Calendar.NOVEMBER;
            case 9:
                return Calendar.DECEMBER;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static int getNumericalMonth(String monthString) {
        final int num1 = monthString.charAt(0) - '0';
        final int num2 = monthString.charAt(1) - '0';

        return num1 * 10 + num2 - 1; // aka Calendar.MMM, where MMM == JANUARY, etc.
    }

    private static int getYear(String yearString) {
        if (yearString.length() == 2) {
            final int year = (yearString.charAt(0) - '0') * 10 + (yearString.charAt(1) - '0');
            if (year >= 70) {
                return year + 1900;
            }

            return year + 2000;
        }

        if (yearString.length() == 3) {
            // According to RFC 2822, three digit years should be added to 1900.
            final int year = (yearString.charAt(0) - '0') * 100 + (yearString.charAt(1) - '0') * 10
                    + (yearString.charAt(2) - '0');
            return year + 1900;
        }

        if (yearString.length() == 4) {
            return (yearString.charAt(0) - '0') * 1000 + (yearString.charAt(1) - '0') * 100
                    + (yearString.charAt(2) - '0') * 10 + (yearString.charAt(3) - '0');
        }

        return 1970;
    }

    private static TimeOfDay getTime(String timeString) {
        int i = 0;

        // HH might be H
        int hour = timeString.charAt(i++) - '0';
        if (timeString.charAt(i) != ':') {
            hour = hour * 10 + (timeString.charAt(i++) - '0');
        }

        // Skip ':'
        ++i;

        final int minute = (timeString.charAt(i++) - '0') * 10 + (timeString.charAt(i++) - '0');

        // Skip ':'
        ++i;

        final int second = (timeString.charAt(i++) - '0') * 10 + (timeString.charAt(i) - '0');

        return new TimeOfDay(hour, minute, second);
    }

    /**
     * @param timeLong time as a long
     * @return time formatted as ISO 8601 ---> YYYY-MM-DDTHH:MM:SS.SSSZ
     */
    public static String getDateTime(long timeLong) {
        StringBuilder httpDateTime = new StringBuilder(128);
        Time time = new Time();
        time.set(timeLong);

        httpDateTime.append(time.year);
        httpDateTime.append('-');
        httpDateTime.append(time.month < 9 ? '0' : "");
        httpDateTime.append(time.month + 1);
        httpDateTime.append('-');
        httpDateTime.append(time.monthDay < 10 ? '0' : "");
        httpDateTime.append(time.monthDay);
        httpDateTime.append('T');
        httpDateTime.append(time.hour < 10 ? '0' : "");
        httpDateTime.append(time.hour);
        httpDateTime.append(':');
        httpDateTime.append(time.minute < 10 ? '0' : "");
        httpDateTime.append(time.minute);
        httpDateTime.append(':');
        httpDateTime.append(time.second < 10 ? '0' : "");
        httpDateTime.append(time.second);
        httpDateTime.append(".000Z");

        return httpDateTime.toString();
    }

    /**
     * Format a numeric date and time using the phone's locale.
     *
     * @param context   the context is required only if the time is shown
     * @param time      a point in time in UTC milliseconds
     * @param separator separator to be placed between date & time
     */
    public static CharSequence formatNumericDateTime(Context context, long time, String separator) {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(DateUtils.formatDateTime(context, time, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE));
        sb.append(separator);
        sb.append(DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME));

        return sb;
    }

    /**
     * Format a date and time using the phone's locale.
     *
     * @param context   the context is required only if the time is shown
     * @param time      a point in time in UTC milliseconds
     * @param separator separator to be placed between date & time
     */
    public static CharSequence formatDateTime(Context context, long time, String separator) {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE));
        sb.append(separator);
        sb.append(DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME));

        return sb;
    }

    public static String formatIsoDate(Context context, String dateString) {
        int day;
        int month;
        int year;
        final Matcher isoMatcher = DATETIME_DATE_ONLY_PATTERN.matcher(dateString);
        if (isoMatcher.find()) {
            year = getYear(isoMatcher.group(1));
            month = getNumericalMonth(isoMatcher.group(2));
            day = getDate(isoMatcher.group(3));
        } else {
            throw new IllegalArgumentException();
        }

        Time date = new Time();
        date.set(day, month, year);

        return DateUtils.formatDateTime(context, date.toMillis(true), DateUtils.FORMAT_NUMERIC_DATE
                | DateUtils.FORMAT_SHOW_DATE);
    }
}