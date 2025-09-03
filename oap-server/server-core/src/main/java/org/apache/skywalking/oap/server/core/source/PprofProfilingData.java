package org.apache.skywalking.oap.server.core.source;

import lombok.Data;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.PPROF_PROFILING_DATA;

@Data
@ScopeDeclaration(id = PPROF_PROFILING_DATA, name = "PprofProfilingData")
@ScopeDefaultColumn.VirtualColumnDefinition(fieldName = "entityId", columnName = "entity_id", isID = true, type = String.class)
public class PprofProfilingData extends Source {
    private volatile String entityId;

    @Override
    public int scope() {
        return PPROF_PROFILING_DATA;
    }

    @Override
    public String getEntityId() {
        if (entityId == null) {
            return taskId + instanceId + eventType + uploadTime;
        }
        return entityId;
    }

    private String taskId;
    private String instanceId;
    private long uploadTime;
    private String eventType;
    private Object frameTree;
} 