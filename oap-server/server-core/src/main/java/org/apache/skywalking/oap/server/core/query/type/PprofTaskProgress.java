package org.apache.skywalking.oap.server.core.query.type;

import lombok.Data;
import org.apache.skywalking.oap.server.core.query.PprofTaskLog;

import java.util.List;

@Data
public class PprofTaskProgress {
    private List<PprofTaskLog> logs;
    private List<String> errorInstanceIds;
    private List<String> successInstanceIds;
}
