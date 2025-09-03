package org.apache.skywalking.oap.query.graphql.resolver;

import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.profiling.pprof.PprofMutationService;
import org.apache.skywalking.oap.server.core.query.type.PprofTaskCreationResult;
import org.apache.skywalking.oap.server.core.query.input.PprofTaskCreationRequest;

import java.io.IOException;

@Slf4j
public class PprofMutation implements GraphQLMutationResolver {
    private final ModuleManager moduleManager;

    private PprofMutationService mutationService;

    public PprofMutation(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private PprofMutationService getPprofMutationService() {
        if (mutationService == null) {
            this.mutationService = moduleManager.find(CoreModule.NAME)
                    .provider()
                    .getService(PprofMutationService.class);
        }
        return mutationService;
    }

    public PprofTaskCreationResult createPprofTask(PprofTaskCreationRequest request) throws IOException {
        PprofMutationService pprofMutationService = getPprofMutationService();
        return pprofMutationService.createTask(request.getServiceId(), request.getServiceInstanceIds(),
                request.getDuration(), request.getEvents(), request.getDumpPeriod());
    }
}