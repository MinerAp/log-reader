package com.amshulman.logreader.state;

import java.time.ZonedDateTime;

import lombok.Value;

@Value
public final class Event {
    ZonedDateTime time;
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
