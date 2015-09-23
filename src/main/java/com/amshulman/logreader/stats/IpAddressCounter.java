package com.amshulman.logreader.stats;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.amshulman.logreader.state.IpAddress;
import com.amshulman.logreader.state.Session;
import com.google.common.collect.ListMultimap;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class IpAddressCounter {

    ListMultimap<String, Session> sessionsByUser;

    public Map<IpAddress, Long> count(String player) {
        return sessionsByUser.get(player)
                             .parallelStream()
                             .map(Session::getIpAddress)
                             .collect(Collectors.groupingByConcurrent(Function.identity(),
                                                                      ConcurrentSkipListMap::new,
                                                                      Collectors.counting()));
    }
}
