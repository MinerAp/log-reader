package com.amshulman.logreader.util;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.amshulman.logreader.state.Session;
import com.amshulman.logreader.state.Session.SessionWithUsername;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Util {

    public static <T> Iterable<T> iterableFromStream(Stream<T> stream) {
        return stream::iterator;
    }

    public static Stream<SessionWithUsername> mapSessionsToUser(String username, Map<String, List<Session>> sessionsByUser) {
        return sessionsByUser.get(username).stream().map(s -> new SessionWithUsername(username, s));
    }

    public static String format(Duration duration) {
        final long days = duration.toHours() / 24;
        final long hours = duration.toHours() % 24;
        final long minutes = duration.toMinutes() % 60;
        final long seconds = duration.getSeconds() % 60;

        final int count = (days > 0 ? 1 : 0) + (hours > 0 ? 1 : 0) + (minutes > 0 ? 1 : 0) + (seconds > 0 ? 1 : 0);
        int remaining = count;

        StringBuilder output = new StringBuilder();

        if (days > 0) {
            output.append(days).append(" day");
            if (days > 1) output.append('s');
            output.append(getConjunction(count, --remaining));
        }

        if (hours > 0) {
            output.append(hours).append(" hour");
            if (hours > 1) output.append('s');
            output.append(getConjunction(count, --remaining));
        }

        if (minutes > 0) {
            output.append(minutes).append(" minute");
            if (minutes > 1) output.append('s');
            output.append(getConjunction(count, --remaining));
        }

        if (seconds > 0) {
            output.append(seconds).append(" second");
            if (seconds > 1) output.append('s');
        }

        return output.toString();
    }

    private static String getConjunction(int count, int remaining) {
        if (count > 2) {
            return remaining > 1 ? ", " : ", and ";
        } else if (count == 2) {
            return remaining == 1 ? " and " : "";
        } else {
            return "";
        }
    }
}
