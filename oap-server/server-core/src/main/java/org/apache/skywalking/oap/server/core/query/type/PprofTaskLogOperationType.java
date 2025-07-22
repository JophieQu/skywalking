package org.apache.skywalking.oap.server.core.query.type;

import java.util.HashMap;
import java.util.Map;

public enum PprofTaskLogOperationType {
    NOTIFIED(1), // when sniffer has execution finished to report
    EXECUTION_FINISHED(2), // when sniffer has execution finished to report
    JFR_UPLOAD_FILE_TOO_LARGE_ERROR(3), // when sniffer finished task but jfr file is to large that oap server can not receive
    EXECUTION_TASK_ERROR(4) // when sniffer fails to execute its task
    ;
    private final int code;
    private static final Map<Integer, PprofTaskLogOperationType> CACHE = new HashMap<Integer, PprofTaskLogOperationType>();

    static {
        for (PprofTaskLogOperationType val : PprofTaskLogOperationType.values()) {
            CACHE.put(val.getCode(), val);
        }
    }

    /**
     * Parse operation type by code
     */
    public static PprofTaskLogOperationType parse(int code) {
        return CACHE.get(code);
    }

    PprofTaskLogOperationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

}
