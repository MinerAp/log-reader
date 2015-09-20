package com.amshulman.logreader.stats;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.amshulman.logreader.state.Session;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AltChecker {

    AddressToPlayerMap playersByAddress;
    PlayerToAddressMap addressesByPlayer;

    public AltChecker(Multimap<String, Session> sessionsByUser) {
        playersByAddress = new AddressToPlayerMap(sessionsByUser);
        addressesByPlayer = new PlayerToAddressMap(sessionsByUser);

    }

    public List<String> findAlts(String username, boolean fuzzyMatch) {
        Stream<String> addresses = addressesByPlayer.getAddresses(username)
                                                    .parallelStream();

        if (fuzzyMatch) {
            addresses = addresses.flatMap(AltChecker::expand);
        }

        return addresses.flatMap(playersByAddress::getPlayers)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList());
    }

    private static Stream<String> expand(String ip) {
        String foo = ip.substring(0, ip.lastIndexOf('.') + 1);
        Stream.Builder<String> bar = Stream.builder();
        for (int i = 0; i < 256; ++i) {
            bar.accept(foo + i);
        }
        return bar.build();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class PlayerToAddressMap {
        ListMultimap<String, String> addresses = ArrayListMultimap.create();

        public PlayerToAddressMap(Multimap<String, Session> sessionsByUser) {
            sessionsByUser.entries()
                          .stream()
                          .forEach(e -> addresses.put(e.getKey(), e.getValue().getIpAddress()));
        }

        public List<String> getAddresses(String username) {
            return addresses.get(username);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class AddressToPlayerMap {
        TIntObjectMap<List<String>> users = new TIntObjectHashMap<>();

        public AddressToPlayerMap(Multimap<String, Session> sessionsByUser) {
            sessionsByUser.entries()
                          .stream()
                          .forEach(e -> addSessionToMap(e.getKey(), e.getValue()));
        }

        public Stream<String> getPlayers(String ip) {
            List<String> players = users.get(convertAddressToInteger(ip));
            return players == null ? Stream.empty() : players.stream();
        }

        private void addSessionToMap(String username, Session session) {
            int ip = convertAddressToInteger(session.getIpAddress());
            List<String> players = users.get(ip);
            if (players == null) {
                players = new ArrayList<>();
                users.put(ip, players);
            }
            players.add(username);
        }

        // Adapted from http://codereview.stackexchange.com/a/84461
        private static int convertAddressToInteger(String ip) {
            int len = ip.length();
            int octet = 0;
            int address = 0;

            for (int i = 0; i < len; ++i) {
                char digit = ip.charAt(i);
                if (digit != '.') {
                    octet = octet * 10 + (digit - '0');
                } else {
                    address = (address << 8) | octet;
                    octet = 0;
                }
            }

            return (address << 8) | octet;
        }
    }

}
