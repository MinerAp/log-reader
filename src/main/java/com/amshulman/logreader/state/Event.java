package com.amshulman.logreader.state;

import java.time.Instant;

import lombok.Value;

@Value
public final class Event {
    Instant time;
    IpAddress ipAddress;
    EventType type;

    @Value
    public static class EventWithUsername {
        String username;
        Event event;
    }

    public static enum EventType {
        LOGIN, LOGOUT
    }
}
