package com.amshulman.logreader.parsing;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.amshulman.logreader.state.Event;
import com.amshulman.logreader.state.Event.EventType;
import com.amshulman.logreader.state.Event.EventWithUsername;
import com.amshulman.logreader.state.IpAddress;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class FileParser {
    static DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                       .withZone(ZoneId.systemDefault());

    static String DATETIME = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    static String INFO = "\\[INFO\\]";
    static String DOT = "\\.";
    static String IPADDRESS = noncapturing("[01]?\\d\\d?|2[0-4]\\d|25[0-5]") + DOT +
                              noncapturing("[01]?\\d\\d?|2[0-4]\\d|25[0-5]") + DOT +
                              noncapturing("[01]?\\d\\d?|2[0-4]\\d|25[0-5]") + DOT +
                              noncapturing("[01]?\\d\\d?|2[0-4]\\d|25[0-5]");
    static String PORT = ":\\d{1,5}";
    static String USERNAME = "[a-zA-Z0-9_]{1,16}";

    static Pattern LOGIN_PATTERN = Pattern.compile("^" +
            DATETIME + " " + INFO + " " +
            capturing(USERNAME) + "\\[/" + capturing(IPADDRESS) + PORT + "\\]" +
            ".*$");

    static Pattern FAILED_CONNECT_PATTERN = Pattern.compile("^" +
            DATETIME + " " + INFO + " " +
            optional("Disconnecting ") + "com\\.mojang\\.authlib\\.GameProfile" +
            ".*name=" + capturing(USERNAME) + ".*\\(/" + capturing(IPADDRESS) + PORT + "\\)" +
            ".*$");

    static Pattern LOGOUT_PATTERN = Pattern.compile("^" +
            DATETIME + " " + INFO + " " +
            capturing(USERNAME) + " lost connection" +
            ".*$");

    static Pattern NOCHEATPLUS_PATTERN = Pattern.compile("^" +
            DATETIME + " " + INFO + " " +
            "\\[NoCheatPlus\\] \\(CONSOLE\\) Kicked " + capturing(USERNAME) +
            ".*$");

    static Pattern DATETIME_PATTERN = Pattern.compile("^" + capturing(DATETIME) + ".*$");

    @NonFinal Instant last = Instant.EPOCH;
    
    public Stream<EventWithUsername> parseLine(String input) {
        Optional<Instant> instantOr = getDateTime(input);
        if (!instantOr.isPresent()) {
            return Stream.empty();
        }
        Instant instant = instantOr.get();
        if (instant.isAfter(last)) {
          last = instant;
        }

        Stream.Builder<EventWithUsername> builder = Stream.builder();
        Matcher matcher;

        // Check to see if the input matches any login pattern
        if ((matcher = LOGIN_PATTERN.matcher(input)).matches() ||
                (matcher = FAILED_CONNECT_PATTERN.matcher(input)).matches()) {
            Event event = new Event(instant, new IpAddress(matcher.group(2)), EventType.LOGIN);
            builder.accept(new EventWithUsername(matcher.group(1), event));
        }

        // Check to see if the input matches any logout pattern
        if ((matcher = LOGOUT_PATTERN.matcher(input)).matches() ||
                (matcher = FAILED_CONNECT_PATTERN.matcher(input)).matches() ||
                (matcher = NOCHEATPLUS_PATTERN.matcher(input)).matches()) {
            Event event = new Event(instant, null, EventType.LOGOUT);
            builder.accept(new EventWithUsername(matcher.group(1), event));
        }

        return builder.build();
    }

    private static Optional<Instant> getDateTime(String input) {
        Matcher matcher = DATETIME_PATTERN.matcher(input);
        if (matcher.matches()) {
            return Optional.of(parse(matcher.group(1)));
        }
        return Optional.empty();
    }

    private static Instant parse(String datetime) {
        return format.parse(datetime, Instant::from);
    }

    private static String capturing(String regex) {
        return "(" + regex + ")";
    }

    private static String noncapturing(String regex) {
        return "(?:" + regex + ")";
    }

    private static String optional(String regex) {
        return noncapturing(regex) + "?";
    }
    
    public Instant getLastInstant() {
        return last;
    }
}
