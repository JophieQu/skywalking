package org.apache.skywalking.oap.server.core.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.query.type.PprofTaskLogOperationType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PprofTaskLog {
    // task id
    private String id;

    // instance
    private String instanceId;
    private String instanceName;

    // operation
    private PprofTaskLogOperationType operationType;
    private long operationTime;
}
