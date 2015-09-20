package com.amshulman.logreader.stats;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.amshulman.logreader.state.Session;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AltChecker {

    AddressToPlayerMap playersByAddress;
    PlayerToAddressMap addressesByPlayer;

    public AltChecker(Multimap<String, Session> sessionsByUser) {
        playersByAddress = new AddressToPlayerMap(sessionsByUser);
        addressesByPlayer = new PlayerToAddressMap(sessionsByUser);
    }

    public List<String> findAlts(String username, List<String> excludedAlts, boolean fuzzyMatch) {
        Stream<String> addresses = addressesByPlayer.getAddresses(username)
                                                    .parallelStream();

        if (fuzzyMatch) {
            addresses = addresses.flatMap(AltChecker::expand);
        }

        Stream<String> alts = addresses.flatMap(playersByAddress::getPlayers)
                                       .distinct();

        if (excludedAlts.contains(username)) {
            alts = alts.filter(username::equals);
        } else {
            alts = alts.filter(name -> !excludedAlts.contains(name));
        }

        return alts.sorted(String.CASE_INSENSITIVE_ORDER)
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
        SetMultimap<String, String> addresses = HashMultimap.create();

        public PlayerToAddressMap(Multimap<String, Session> sessionsByUser) {
            sessionsByUser.entries()
                          .stream()
                          .forEach(e -> addresses.put(e.getKey(), e.getValue().getIpAddress()));
        }

        public Collection<String> getAddresses(String username) {
            return addresses.get(username);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class AddressToPlayerMap {
        TIntObjectMap<Set<String>> users = new TIntObjectHashMap<>();

        public AddressToPlayerMap(Multimap<String, Session> sessionsByUser) {
            sessionsByUser.entries()
                          .stream()
                          .forEach(e -> addSessionToMap(e.getKey(), e.getValue()));
        }

        public Stream<String> getPlayers(String ip) {
            Set<String> players = users.get(convertAddressToInteger(ip));
            return players == null ? Stream.empty() : players.stream();
        }

        private void addSessionToMap(String username, Session session) {
            int ip = convertAddressToInteger(session.getIpAddress());
            Set<String> players = users.get(ip);
            if (players == null) {
                players = new HashSet<>();
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
