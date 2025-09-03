package org.apache.skywalking.oap.server.core.profiling.pprof;


import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofTaskQueryDAO;
import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofDataQueryDAO;
import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofTaskLogQueryDAO;
import org.apache.skywalking.oap.server.core.query.PprofTaskLog;
import org.apache.skywalking.oap.server.core.query.input.Duration;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import java.util.Objects;
import org.apache.skywalking.oap.server.core.profiling.pprof.storage.PprofProfilingDataRecord;
import org.apache.skywalking.oap.server.core.query.type.PprofEventType;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PprofQueryService implements Service {
    private final ModuleManager moduleManager;

    private IPprofTaskQueryDAO taskQueryDAO;
    private IPprofDataQueryDAO dataQueryDAO;
    private IPprofTaskLogQueryDAO logQueryDAO;

    private IPprofTaskQueryDAO getTaskQueryDAO() {
        if (taskQueryDAO == null) {
            this.taskQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IPprofTaskQueryDAO.class);
        }
        return taskQueryDAO;
    }

    private IPprofDataQueryDAO getPprofDataQueryDAO() {
        if (dataQueryDAO == null) {
            this.dataQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IPprofDataQueryDAO.class);
        }
        return dataQueryDAO;
    }

    private IPprofTaskLogQueryDAO getTaskLogQueryDAO() {
        if (logQueryDAO == null) {
            this.logQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IPprofTaskLogQueryDAO.class);
        }
        return logQueryDAO;
    }

    public List<PprofTask> queryTask(String serviceId, Duration duration, Integer limit) throws IOException {
        Long startTimeBucket = null;
        Long endTimeBucket = null;
        if (Objects.nonNull(duration)) {
            startTimeBucket = duration.getStartTimeBucketInSec();
            endTimeBucket = duration.getEndTimeBucketInSec();
        }

        return getTaskQueryDAO().getTaskList(serviceId, startTimeBucket, endTimeBucket, limit);
    }

    public List<PprofTaskLog> queryPprofTaskLogs(String taskId) throws IOException {
        List<PprofTaskLog> taskLogList = getTaskLogQueryDAO().getTaskLogList();
        return findMatchedLogs(taskId, taskLogList);
    }
    
    private List<PprofTaskLog> findMatchedLogs(final String taskID, final List<PprofTaskLog> allLogs) {
        return allLogs.stream()
                .filter(l -> Objects.equals(l.getId(), taskID))
                .map(this::extendTaskLog)
                .collect(Collectors.toList());
    }

    private PprofTaskLog extendTaskLog(PprofTaskLog log) {
        final IDManager.ServiceInstanceID.InstanceIDDefinition instanceIDDefinition = IDManager.ServiceInstanceID
                .analysisId(log.getInstanceId());
        log.setInstanceName(instanceIDDefinition.getName());
        return log;
    }
    
}
