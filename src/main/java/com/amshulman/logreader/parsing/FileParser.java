package com.amshulman.logreader.parsing;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.amshulman.logreader.Main;
import com.amshulman.logreader.state.Event;
import com.amshulman.logreader.state.Event.EventType;
import com.amshulman.logreader.state.Event.EventWithUsername;
import com.amshulman.logreader.state.IpAddress;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class FileParser {
    static DateTimeFormatter baseDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static DateTimeFormatter norwayDateTimeFormatter = baseDateTimeFormatter.withZone(ZoneId.of("Europe/Oslo"));
    static DateTimeFormatter germanyDateTimeFormatter = baseDateTimeFormatter.withZone(ZoneId.of("Europe/Berlin"));
    static DateTimeFormatter newYorkDateTimeFormatter = baseDateTimeFormatter.withZone(ZoneId.of("America/New_York"));

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
            capturing(USERNAME) + " ?\\[/" + capturing(IPADDRESS) + PORT + "\\]" +
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

    @NonFinal @Getter ZonedDateTime lastTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);

    public Stream<EventWithUsername> parseLine(String input) {
        Optional<ZonedDateTime> dateTimeOr = getDateTime(input);
        if (!dateTimeOr.isPresent()) {
            return Stream.empty();
        }
        ZonedDateTime dateTime = dateTimeOr.get();
        if (dateTime.isBefore(lastTime)) {
          ZonedDateTime adjusted = dateTime.plus(Duration.ofHours(1));
          if (!dateTime.getOffset().equals(adjusted.getOffset())) {
            // Time flowed backwards because of DST.
            dateTime = adjusted;
          } else if (Main.DEBUG) {
            System.err.println("Time flowed backwards at " + dateTime + " for " + Duration.between(dateTime, lastTime).getSeconds() + " seconds.");
          }
        }

        if (dateTime.isAfter(lastTime)) {
          lastTime = dateTime;
        }

        Stream.Builder<EventWithUsername> builder = Stream.builder();
        Matcher matcher;

        // Check to see if the input matches any login pattern
        if ((matcher = LOGIN_PATTERN.matcher(input)).matches() ||
                (matcher = FAILED_CONNECT_PATTERN.matcher(input)).matches()) {
            Event event = new Event(dateTime, new IpAddress(matcher.group(2)), EventType.LOGIN);
            builder.accept(new EventWithUsername(matcher.group(1), event));
        }

        // Check to see if the input matches any logout pattern
        if ((matcher = LOGOUT_PATTERN.matcher(input)).matches() ||
                (matcher = FAILED_CONNECT_PATTERN.matcher(input)).matches() ||
                (matcher = NOCHEATPLUS_PATTERN.matcher(input)).matches()) {
            Event event = new Event(dateTime, null, EventType.LOGOUT);
            builder.accept(new EventWithUsername(matcher.group(1), event));
        }

        return builder.build();
    }

    private static Optional<ZonedDateTime> getDateTime(String input) {
        Matcher matcher = DATETIME_PATTERN.matcher(input);
        if (matcher.matches()) {
            if (input.compareTo("2012-11-07") < 0) {
              // The first log entry from the server running in Germany was on 2012-11-08 15:19:33.
              // However, many logs in this time period were lost. In the extremely unlikely event they
              // are ever recovered, this threshold may need to be adjusted.
              return Optional.of(norwayDateTimeFormatter.parse(matcher.group(1), ZonedDateTime::from));
            } else if (input.compareTo("2016-02-21") < 0) {
              // The last log entry from the server running in Germany was on 2016-02-20 23:59:22.
              return Optional.of(germanyDateTimeFormatter.parse(matcher.group(1), ZonedDateTime::from));
            } else {
              return Optional.of(newYorkDateTimeFormatter.parse(matcher.group(1), ZonedDateTime::from));
            }
        }
        return Optional.empty();
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
}
