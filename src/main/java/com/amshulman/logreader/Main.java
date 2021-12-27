package com.amshulman.logreader;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.javatuples.Pair;

import com.amshulman.logreader.parsing.LogParser;
import com.amshulman.logreader.state.Session;
import com.amshulman.logreader.stats.AltChecker;
import com.amshulman.logreader.stats.IpAddressCounter;
import com.amshulman.logreader.stats.Playtime;
import com.amshulman.logreader.util.Util;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ListMultimap;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class Main {

    public static final boolean DEBUG = false;

    public static void main(String[] args) {
        Parameters params = Parameters.parse(args);

        if (!params.hasPaths()) {
            System.err.println("Please provide one or more paths");
            return;
        }

        if (!params.hasCommand()) {
            System.err.println("Please provide one or more commands");
            return;
        }

        LogParser parser = new LogParser(params.getPaths());
        ListMultimap<String, Session> sessionsByUser = parser.readLogs(params.recurse);

        if (!params.getAlts().isEmpty()) {
            AltChecker altChecker = new AltChecker(sessionsByUser);
            System.out.println("====== Alts ======");
            params.getAlts()
                  .stream()
                  .sorted(String.CASE_INSENSITIVE_ORDER)
                  .map(username -> username + ": " + altChecker.findAlts(username, params.excludedAlts, false))
                  .forEachOrdered(System.out::println);
        }

        if (!params.getIpCounts().isEmpty()) {
            IpAddressCounter counter = new IpAddressCounter(sessionsByUser);
            System.out.println("====== IP Address Counts ======");
            params.getIpCounts()
                  .stream()
                  .sorted(String.CASE_INSENSITIVE_ORDER)
                  .map(username -> username + ": " + counter.count(username))
                  .forEachOrdered(System.out::println);
        }

        if (!params.getPlaytime().isEmpty()) {
            Playtime playtimeCalculator = new Playtime(sessionsByUser);
            System.out.println("====== Playtime ======");
            params.getPlaytime()
                  .stream()
                  .map(username -> new Pair<String, Duration>(username,
                                                              playtimeCalculator.getPlaytime(username)))
                  .sorted(Comparator.comparing(Pair<String, Duration>::getValue1).reversed())
                  .map(p -> p.getValue0() + ": " + Util.formatDuration(p.getValue1()))
                  .forEachOrdered(System.out::println);
        }

//        Analytics.run(sessionsByUser);

//        InGameUsers inGameUsers = new InGameUsers(sessionsByUser);
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Parameters {

        @Parameter List<String> paths = new ArrayList<>();
        @Parameter(names = { "-alt" }) List<String> alts = new ArrayList<>();
        @Parameter(names = { "-excludeAlt" }) List<String> excludedAlts = new ArrayList<>();
        @Parameter(names = { "-ipCounts" }) List<String> ipCounts = new ArrayList<>();
        @Parameter(names = { "-playtime" }) List<String> playtime = new ArrayList<>();
        @Parameter(names = { "-recurse"}, arity = 1) boolean recurse = true;

        public static Parameters parse(String[] args) {
            Parameters params = new Parameters();
            new JCommander(params).parse(args);
            return params;
        }

        public boolean hasPaths() {
            return !paths.isEmpty();
        }

        public boolean hasCommand() {
            return !(alts.isEmpty() && ipCounts.isEmpty() && playtime.isEmpty());
        }
    }
}
