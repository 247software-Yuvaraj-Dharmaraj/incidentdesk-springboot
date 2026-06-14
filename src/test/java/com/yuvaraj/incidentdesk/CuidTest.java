package com.yuvaraj.incidentdesk;

import com.yuvaraj.incidentdesk.util.Cuid;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuidTest {

    @Test
    void startsWithCAndIsUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            String id = Cuid.generate();
            assertTrue(id.startsWith("c"), "id should start with 'c'");
            ids.add(id);
        }
        assertEquals(5000, ids.size(), "all ids should be unique");
    }
}
