package com.amshulman.logreader.state;

import java.time.Instant;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(of = { "login", "logout" })
public final class Session implements Comparable<Session> {
    Instant login;
    Instant logout;
    IpAddress ipAddress;

    public Session(Event login, Event logout) {
        this(login, logout.getTime());
    }

    public Session(Event login, Instant logout) {
        this.login = login.getTime();
        this.logout = logout;
        ipAddress = login.getIpAddress();
    }

    @Override
    public int compareTo(Session other) {
        int cmp = login.compareTo(other.login);
        if (cmp != 0) {
            return cmp;
        }
        return logout.compareTo(other.logout);
    }

    @Value
    public static class SessionWithUsername {
        String username;
        Session session;
    }
}
