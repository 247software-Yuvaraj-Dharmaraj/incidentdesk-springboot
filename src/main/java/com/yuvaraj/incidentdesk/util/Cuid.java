package com.yuvaraj.incidentdesk.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collision-resistant, sortable-ish string IDs in the spirit of cuid
 * (prefix 'c' + base36 timestamp + counter + fingerprint + random).
 * Kept so IDs visually match the original Prisma-generated records.
 */
public final class Cuid {

    private static final int BASE = 36;
    private static final int BLOCK = 4;
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String FINGERPRINT =
            pad(Long.toString(ProcessHandle.current().pid() & 0xFFFFFF, BASE)) + pad(Integer.toString(hostHash(), BASE));

    private Cuid() {
    }

    public static String generate() {
        String timestamp = Long.toString(System.currentTimeMillis(), BASE);
        String counter = pad(Integer.toString(COUNTER.getAndIncrement() & 0xFFFFFF, BASE));
        String random = pad(Integer.toString(RANDOM.nextInt(0xFFFFFF), BASE))
                + pad(Integer.toString(RANDOM.nextInt(0xFFFFFF), BASE));
        return ("c" + timestamp + counter + FINGERPRINT + random);
    }

    private static int hostHash() {
        String host = System.getenv().getOrDefault("HOSTNAME", "incidentdesk");
        return Math.abs(host.hashCode()) & 0xFFFFFF;
    }

    private static String pad(String input) {
        if (input.length() == BLOCK) {
            return input;
        }
        if (input.length() > BLOCK) {
            return input.substring(input.length() - BLOCK);
        }
        return "0".repeat(BLOCK - input.length()) + input;
    }
}
