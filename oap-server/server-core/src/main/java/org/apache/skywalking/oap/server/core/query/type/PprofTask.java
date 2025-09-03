package org.apache.skywalking.oap.server.core.query.type;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PprofTask {
    
    private String id;
    private String serviceId;
    private List<String> serviceInstanceIds;
    private long createTime;
    private long startTime;
    private PprofEventType events;
    private int duration;
    private int dumpPeriod;

}