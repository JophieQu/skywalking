package org.apache.skywalking.oap.server.core.query.input;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.query.type.PprofEventType;

import java.util.List;

@Getter
@Setter
public class PprofTaskCreationRequest {
    private String serviceId;
    private List<String> serviceInstanceIds;
    private int duration;
    private PprofEventType events;
    private int dumpPeriod;
}
