package com.amshulman.logreader;

import com.amshulman.logreader.parsing.LogParser;
import com.amshulman.logreader.state.Session;
import com.google.common.collect.ListMultimap;

public final class Main {

    public static final boolean DEBUG = false;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please provide one or more paths");
            return;
        }

        LogParser parser = new LogParser(args);
        ListMultimap<String, Session> sessionsByUser = parser.readLogs();

//        AltChecker altChecker = new AltChecker(sessionsByUser);

//        Playtime playtime = new Playtime(sessionsByUser);
//        Duration d = playtime.getPlaytime("tayler1986");
//        System.out.println(Util.format(d));

//        Analytics.run(sessionsByUser);

//        InGameUsers inGameUsers = new InGameUsers(sessionsByUser);
//        System.out.println(foo);
    }

}
