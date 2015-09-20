package com.amshulman.logreader;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.amshulman.logreader.parsing.LogParser;
import com.amshulman.logreader.state.Session;
import com.amshulman.logreader.stats.AltChecker;
import com.amshulman.logreader.stats.Playtime;
import com.amshulman.logreader.util.Util;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ListMultimap;

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
        ListMultimap<String, Session> sessionsByUser = parser.readLogs();

        if (!params.getAlts().isEmpty()) {
            AltChecker altChecker = new AltChecker(sessionsByUser);
            System.out.println("====== Alts ======");
            for (String username : params.getAlts()) {
                List<String> alts = altChecker.findAltsExact(username);
                System.out.println(username + ": " + alts.toString());
            }
        }

        if (!params.getPlaytime().isEmpty()) {
            Playtime playtimeCalculator = new Playtime(sessionsByUser);
            System.out.println("====== Playtime ======");
            for (String username : params.getPlaytime()) {
                String playtime = Util.formatDuration(playtimeCalculator.getPlaytime(username));
                System.out.println(username + ": " + playtime);
            }
        }

//        Analytics.run(sessionsByUser);

//        InGameUsers inGameUsers = new InGameUsers(sessionsByUser);
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Parameters {

        @Parameter List<String> paths = new ArrayList<>();
        @Parameter(names = { "-alt" }) List<String> alts = new ArrayList<>();
        @Parameter(names = { "-playtime" }) List<String> playtime = new ArrayList<>();

        public static Parameters parse(String[] args) {
            Parameters params = new Parameters();
            new JCommander(params, args);
            return params;
        }

        public boolean hasPaths() {
            return !paths.isEmpty();
        }

        public boolean hasCommand() {
            return !(alts.isEmpty() && playtime.isEmpty());
        }
    }
}
