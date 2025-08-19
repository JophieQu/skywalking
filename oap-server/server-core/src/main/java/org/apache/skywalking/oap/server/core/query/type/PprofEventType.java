package org.apache.skywalking.oap.server.core.query.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum PprofEventType {
    CPU(0, "cpu"),
    Memory(1, "memory"),
    Block(2, "block"),
    Mutex(3, "mutex");


    private final int code;
    private final String name;

    public static PprofEventType valueOfString(String event) {
        return PprofEventType.valueOf(event);
    }
}
