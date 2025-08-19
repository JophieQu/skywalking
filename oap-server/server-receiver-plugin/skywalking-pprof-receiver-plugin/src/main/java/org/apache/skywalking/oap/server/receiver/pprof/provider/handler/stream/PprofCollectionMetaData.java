package org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream;

import org.apache.skywalking.oap.server.core.query.type.PprofTask;

import org.apache.skywalking.apm.network.pprof.v10.PprofProfilingStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PprofCollectionMetaData {
    private PprofTask task;
    private String serviceId;
    private String instanceId;
    private int contentSize;
    private PprofProfilingStatus type;
    private long uploadTime;
}
