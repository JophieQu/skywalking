package org.apache.skywalking.oap.query.graphql.resolver;
import org.apache.skywalking.oap.server.core.CoreModule;
import groovy.util.logging.Slf4j;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.apache.skywalking.oap.server.core.profiling.pprof.PprofQueryService;
import org.apache.skywalking.oap.server.core.query.type.PprofTaskListResult;
import org.apache.skywalking.oap.server.core.query.input.PprofTaskListRequest;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.oap.server.core.query.type.PprofTaskProgress;
import org.apache.skywalking.oap.server.core.query.PprofTaskLog;
import org.apache.skywalking.oap.server.core.query.type.PprofTaskLogOperationType;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;

@Slf4j
public class PprofQuery implements GraphQLQueryResolver {
    private final ModuleManager moduleManager;

    private PprofQueryService queryService;

    public PprofQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private PprofQueryService getPprofQueryService() {
        if (queryService == null) {
            this.queryService = moduleManager.find(CoreModule.NAME).provider().getService(PprofQueryService.class);
        }
        return queryService;
    }

    public PprofTaskListResult queryPprofTaskList(PprofTaskListRequest request) throws IOException {
        List<PprofTask> tasks = getPprofQueryService().queryTask(
                request.getServiceId(), request.getQueryDuration(), request.getLimit()
        );
        return new PprofTaskListResult(null, tasks);
    }

    public PprofTaskProgress queryPprofTaskProgress(String taskId) throws IOException {
        PprofTaskProgress pprofTaskProgress = new PprofTaskProgress();
        List<PprofTaskLog> logs = getPprofQueryService().queryPprofTaskLogs(taskId);
        pprofTaskProgress.setLogs(logs);
        List<String> errorInstances = new ArrayList<>();
        List<String> successInstances = new ArrayList<>();
        logs.forEach(log -> {
            if (PprofTaskLogOperationType.EXECUTION_FINISHED.equals(log.getOperationType())) {
                successInstances.add(log.getInstanceId());
            } else if (PprofTaskLogOperationType.EXECUTION_TASK_ERROR.equals(log.getOperationType())
                    || PprofTaskLogOperationType.PPROF_UPLOAD_FILE_TOO_LARGE_ERROR.equals(log.getOperationType())) {
                errorInstances.add(log.getInstanceId());
            }
        });
        pprofTaskProgress.setErrorInstanceIds(errorInstances);
        pprofTaskProgress.setSuccessInstanceIds(successInstances);
        return pprofTaskProgress;
    }
}
