package com.amshulman.logreader.state;

import lombok.Value;

@Value
public final class IpAddress implements Comparable<IpAddress> {

    int address;

    // Adapted from http://codereview.stackexchange.com/a/84461
    public IpAddress(String ip) {
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

        this.address = (address << 8) | octet;
    }

    @Override
    public int compareTo(IpAddress other) {
        return toString().compareTo(other.toString());
    }

    @Override
    public String toString() {
        int a = 0xFF & (address >> 24);
        int b = 0xFF & (address >> 16);
        int c = 0xFF & (address >> 8);
        int d = 0xFF & address;

        return a + "." + b + "." + c + "." + d;
    }
}
