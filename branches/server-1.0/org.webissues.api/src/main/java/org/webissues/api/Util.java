package org.webissues.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Various utility methods.
 */
public class Util {

    /**
     * Parse a string as seconds since the Unix epoch. <code>null</code> will be
     * returned if the string is <code>null</code>
     * 
     * @param timestamp timestamp as seconds since unix epoch
     * @return date
     */
    public static Calendar parseTimestampInSeconds(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(Long.parseLong(timestamp) * 1000);
        return now;
    }

    /**
     * Parse a string as milliseconds since the Unix epoch. <code>null</code>
     * will be returned if the string is <code>null</code>
     * 
     * @param timestamp timestamp as milliseconds since unix epoch
     * @return date
     */
    public static Calendar parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(Long.parseLong(timestamp));
        return now;
    }

    /**
     * Escape a string so it is suitable for sending to the WebIssues server as
     * quoted text. Newline (\n) characters, single quotes (') and backslashes
     * (\) will be escaped with a backslash.
     * 
     * @param text text to escape
     * @return escaped text
     */
    public static String escape(String text) {
        return text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
    }

    /**
     * Parse a string that is of the format returned by WebIssues server into a
     * list of arguments. Each argument is separated by a space. Arguments that
     * contain spaces are wrapped in single quotes ('), and single quotes,
     * newline characters and backslashes are all escaped with a backslash.
     * 
     * @param line line to parse
     * @return parsed arguments
     * @throws IllegalArgumentException if the line cannot be parsed
     */
    public static List<String> parseLine(String line) {
        return parseLine(line, ' ', '\'');
    }

    /**
     * Parse a delimited string whose arguments may be quoted and the contents
     * of those quotes escaped.
     * 
     * @param line line to parse
     * @param delimiter character
     * @param quote quote character
     * @return parsed arguments
     * @throws IllegalArgumentException if the line cannot be parsed
     */
    public static List<String> parseLine(String line, char delimiter, char quote) {
        List<String> args = new ArrayList<String>();
        int length = line.length();
        char ch;
        boolean inQuote = false;
        boolean escaped = false;
        boolean inGroup = false;
        boolean argExists = false;
        StringBuilder arg = new StringBuilder();
        for (int i = 0; i < length; i++) {
            ch = line.charAt(i);
            if (ch == delimiter && !inQuote && !escaped && !inGroup) {
                args.add(arg.toString());
                arg.setLength(0);
            } else if (ch == '\\' && !escaped && !inGroup) {
                escaped = true;
            } else if (ch == quote && !escaped && !inGroup) {
                inQuote = !inQuote;
                argExists = true;
            } else if (ch == 'n' && escaped) {
                arg.append('\n');
                argExists = true;
                escaped = false;
            } else if (ch == '{' && !escaped && !inQuote && !inGroup) {
                arg.append(ch);
                argExists = true;
                inGroup = true;
            } else if (ch == '}' && !escaped && !inQuote && inGroup) {
                arg.append(ch);
                argExists = true;
                inGroup = false;
            } else {
                arg.append(ch);
                argExists = true;
                escaped = false;
            }
        }
        if (inQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in '" + line + "'");
        }
        if (inGroup) {
            throw new IllegalArgumentException("Unbalanced braces in '" + line + "'");
        }
        if (escaped) {
            throw new IllegalArgumentException("Incomplete escape sequence.");
        }
        if (argExists) {
            args.add(arg.toString());
        }
        return args;
    }

    /**
     * Parse a date / time using the default date/time format.
     * 
     * @param dateTime date time text
     * @return calendar
     */
    public static Calendar parseDateTimeToCalendar(String dateTime) {
        try {
            return dateTime == null ? null : toCalendar(DateFormat.getDateTimeInstance().parse(dateTime));
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Format a calendar using the default date/time format.
     * 
     * @param c calendar to format
     * @return formatted calendar
     */
    public static String formatDateTime(Calendar c) {
        return c == null ? null : formatDateTime(c.getTime());
    }

    /**
     * Format a date using the default date/time format.
     * 
     * @param d date to format
     * @return formatted date in seconds since the unix epoch
     */
    public static String formatDateTime(Date d) {
        return d == null ? null : DateFormat.getDateTimeInstance().format(d);
    }

    /**
     * Format a calendar as seconds since the unix epoch.
     * 
     * @param cal calendar to format
     * @return formatted calendar in seconds since the unix epoch
     */
    public static String formatTimestampInSeconds(Calendar cal) {
        return cal == null ? null : formatTimestampInSeconds(cal.getTime());
    }

    /**
     * Format a date as seconds since the unix epoch.
     * 
     * @param date date to format
     * @return formatted date in seconds since the unix epoch
     */
    public static String formatTimestampInSeconds(Date date) {
        return date == null ? null : String.valueOf(date.getTime() / 1000);
    }

    /**
     * Format a calendar as milliseconds since the unix epoch.
     * 
     * @param cal calendar to format
     * @return formatted calendar in milliseconds since the unix epoch
     */
    public static String formatTimestamp(Calendar cal) {
        return cal == null ? null : formatTimestamp(cal.getTime());
    }

    /**
     * Format a date as milliseconds since the unix epoch.
     * 
     * @param date date to format
     * @return formatted date in milliseconds since the unix epoch
     */
    public static String formatTimestamp(Date date) {
        return date == null ? null : String.valueOf(date.getTime());
    }

    /**
     * Get if a string is <code>null</code> or of zero length.
     * 
     * @param string string
     * @return is null or blank
     */
    public static boolean isNullOrBlank(String string) {
        return string == null || string.trim().length() == 0;
    }

    /**
     * Convert a date to a calendar.
     * 
     * @param date date
     * @return calendar
     */
    public static Calendar toCalendar(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    /**
     * Concatenate elements of a URI. Any spurious slashes are removed from the
     * begining and end of each element.
     * 
     * @param elements elements of URI
     * @return full URI
     */
    public static String concatenateUri(String... elements) {
        StringBuffer buf = new StringBuffer();
        for (String element : elements) {
            if (buf.length() == 0) {
                buf.append(element);
            } else {
                if (!buf.toString().endsWith("/")) {
                    buf.append("/");
                }
                buf.append(element = trimElementStart(element));
            }
        }
        return buf.toString();
    }

    private static String trimElementStart(String element) {
        while (element.startsWith("/")) {
            element = element.substring(1);
        }
        return element;
    }

    /**
     * Return a blank string if the provided string is <code>null</code>,
     * otherwise just return the original string.
     * 
     * @param text text
     * @return non null text
     */
    public static String nonNull(String text) {
        return text == null ? "" : text;
    }

    /**
     * Encode a URL. This simple turns the {@link UnsupportedEncodingException}
     * thrown by {@link URLEncoder#encode(String, String)} into an {@link Error}
     * to save the caller having to explicitly catch this incredibly rare
     * exception.
     * 
     * @param text
     * @return encoded text
     */
    public static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    /**
     * Format a a list of objects as a delimited string. Each objects
     * {@link Object#toString()} method is called for the content of each
     * element.
     * 
     * @param delimiter delimited
     * @param args arguments
     * @return delimited string
     */
    public static String toDelimitedString(String delimiter, Object... args) {
        StringBuilder bui = new StringBuilder();
        for (Object arg : args) {
            if (bui.length() != 0) {
                bui.append(delimiter);
            }
            bui.append(arg.toString());
        }
        return bui.toString();
    }

    /**
     * Format a list of {@link Entity}'s ID attribute as a delimited string.
     * 
     * @param delimiter delimited
     * @param args entities
     * @return delimited string
     */
    public static String toDelimitedIdString(String delimiter, Entity... args) {
        StringBuilder bui = new StringBuilder();
        for (Entity arg : args) {
            if (bui.length() != 0) {
                bui.append(delimiter);
            }
            bui.append(arg.getId());
        }
        return bui.toString();
    }

    /**
     * Get an array of the indexes where the 'selected' elements appear in the
     * 'from' list.
     * 
     * @param selected selected elements
     * @param from list to get indexes from
     * @return indexes of selected elements in from list
     */
    public static int[] getSelectedIndex(String[] selected, List<String> from) {
        int[] sel = new int[selected.length];
        int idx = 0;
        for (String name : selected) {
            sel[idx++] = from.indexOf(name);
        }
        return sel;
    }

    /**
     * Get a list of entity names from the list of entities.
     * 
     * @param values entities
     * @return list of entity names
     */
    public static Collection<String> getEntityNames(Collection<? extends NamedEntity> values) {
        List<String> names = new ArrayList<String>();
        for (NamedEntity ne : values) {
            names.add(ne.getName());
        }
        return names;
    }
}
