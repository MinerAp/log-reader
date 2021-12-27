package com.amshulman.logreader.parsing;

import static com.amshulman.logreader.Main.DEBUG;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.amshulman.logreader.io.FileUtil;
import com.amshulman.logreader.state.Event;
import com.amshulman.logreader.state.Event.EventType;
import com.amshulman.logreader.state.Event.EventWithUsername;
import com.amshulman.logreader.state.Session;
import com.amshulman.logreader.state.Session.SessionWithUsername;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.PeekingIterator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LogParser {

    Path[] paths;

    public LogParser(List<String> paths) {
        this.paths = paths.stream()
                          .map(Paths::get)
                          .toArray(Path[]::new);
    }

    public ListMultimap<String, Session> readLogs(boolean recurse) {
        ListMultimap<String, Session> m = ArrayListMultimap.create();
        Arrays.stream(paths)
              .flatMap(p -> FileUtil.walk(p, recurse))
              .flatMap(LogParser::processLinesInOrder)
              .sequential()
              .forEach(s -> m.put(s.getUsername(), s.getSession()));
        return Multimaps.unmodifiableListMultimap(m);
    }

    private static Stream<SessionWithUsername> processLinesInOrder(Path path) {
        FileParser fileParser = new FileParser();
        return FileUtil.lines(path)
                       .flatMap(l -> fileParser.parseLine(l))
                       .collect(Collectors.groupingBy(EventWithUsername::getUsername,
                                                      Collectors.mapping(EventWithUsername::getEvent, Collectors.toList())))
                       .entrySet()
                       .parallelStream()
                       .flatMap(e -> convertEventsToSessions(e.getKey(), e.getValue(), fileParser.getLastInstant()));
    }

    private static Stream<SessionWithUsername> convertEventsToSessions(String username, List<Event> events, Instant fileClose) {
        Stream.Builder<SessionWithUsername> stream = Stream.builder();
        for (PeekingIterator<Event> iter = Iterators.peekingIterator(events.iterator()); iter.hasNext();) {
            Event start = iter.next();
            // Ensure start is a LOGIN event. If it's not, we ignore it.
            if (start.getType() != EventType.LOGIN) {
                if (DEBUG) {
                    System.err.println("Expected LOGIN event for " + username + " at " + start.getTime() + ":\n" + events);
                }
                continue;
            }

            // If there are no more elements, we can't find a matching LOGOUT element.
            if (!iter.hasNext()) {
                if (DEBUG) {
                    System.err.println("Found dangling login for " + username + " at " + start.getTime());
                }
                stream.accept(new SessionWithUsername(username, new Session(start, fileClose)));
                break;
            }

            // Ensure the next element is a LOGOUT event. If it's not, we ignore the login event.
            if (iter.peek().getType() != EventType.LOGOUT) {
                if (DEBUG) {
                    System.err.println("Expected LOGOUT event for " + username + " at " + iter.peek().getTime() + ":\n" + events);
                }
                continue;
            }

            Event end = iter.next();
            stream.accept(new SessionWithUsername(username, new Session(start, end)));
        }

        return stream.build();
    }
}
