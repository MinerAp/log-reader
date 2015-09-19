package com.amshulman.logreader.stats;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.amshulman.logreader.state.Session;
import com.google.common.collect.ListMultimap;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Playtime {

    ListMultimap<String, Session> sessionsByUser;

    public Duration getPlaytime(String username) {
        return sessionsByUser.get(username)
                             .parallelStream()
                             .map(s -> Duration.between(s.getLogin(), s.getLogout()))
                             .reduce((a, b) -> a.plus(b)).orElse(Duration.ZERO);
    }
}
