package com.amshulman.logreader.stats;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.amshulman.logreader.state.Session;
import com.google.common.collect.ListMultimap;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Analytics {

    ListMultimap<String, Session> sessionsByUser;

    public static void run(ListMultimap<String, Session> sessionsByUser) {
        Analytics analytics = new Analytics(sessionsByUser);
        long userCount = analytics.sessionsByUser.keySet().size();
        long sessionCount = sessionsByUser.values().size();

        System.out.println("Users count: " + userCount);
        System.out.println("Session count: " + sessionCount);
        System.out.println("Average sessions/user: " + sessionCount / (double) userCount);
        System.out.println("Average session length: " + analytics.getAverageSessionLengthInMinutes(Optional.empty()) + " minutes");
        System.out.println("Average session length: " + analytics.getAverageSessionLengthInMinutes(Optional.of(Duration.ofMinutes(1))) + " minutes");
    }

    private double getAverageSessionLengthInMinutes(Optional<Duration> minimumLength) {
        Stream<Duration> sessionDurations = sessionsByUser.values().parallelStream().map(s -> Duration.between(s.getLogin(), s.getLogout()));
        if (minimumLength.isPresent()) {
            sessionDurations = sessionDurations.filter(d -> minimumLength.get().compareTo(d) <= 0);
        }
        return sessionDurations.mapToLong(Duration::toMinutes).average().orElse(-1);
    }
}
