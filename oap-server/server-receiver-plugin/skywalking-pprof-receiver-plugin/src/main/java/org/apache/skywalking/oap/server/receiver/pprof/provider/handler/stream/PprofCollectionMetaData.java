package org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PprofCollectionMetaData {
    private PprofTask task;
    private String serviceId;
    private String instanceId;
    private String type;
    private int contentSize;
    private long uploadTime;
} 