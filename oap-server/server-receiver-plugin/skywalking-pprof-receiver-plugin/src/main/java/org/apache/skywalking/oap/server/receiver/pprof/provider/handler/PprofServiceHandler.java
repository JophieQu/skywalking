package org.apache.skywalking.oap.server.receiver.pprof.provider.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofData;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofMetaData;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofTaskCommandQuery;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofTaskGrpc;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.core.query.type.PprofTaskLogOperationType;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream.PprofByteBufCollectionObserver;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream.PprofCollectionMetaData;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream.PprofFileCollectionObserver;

import java.io.IOException;

@Slf4j
public class PprofServiceHandler extends PprofTaskGrpc.PprofTaskImplBase implements GRPCHandler {

    private final SourceReceiver sourceReceiver;
    private final int pprofMaxSize;
    private final boolean memoryParserEnabled;

    public PprofServiceHandler(ModuleManager moduleManager, int pprofMaxSize, boolean memoryParserEnabled) {
        this.sourceReceiver = moduleManager.find(CoreModule.NAME).provider().getService(SourceReceiver.class);
        this.pprofMaxSize = pprofMaxSize;
        this.memoryParserEnabled = memoryParserEnabled;
    }

    @Override
    public StreamObserver<PprofData> collect(StreamObserver<Commands> responseObserver) {
        return memoryParserEnabled ?
                new PprofByteBufCollectionObserver(null, responseObserver, sourceReceiver, pprofMaxSize)
                : new PprofFileCollectionObserver(null, responseObserver, sourceReceiver, pprofMaxSize);
    }

    @Override
    public void getPprofTaskCommands(PprofTaskCommandQuery request, StreamObserver<Commands> responseObserver) {
        String serviceId = IDManager.ServiceID.buildId(request.getService(), true);
        String serviceInstanceId = IDManager.ServiceInstanceID.buildId(serviceId, request.getServiceInstance());

        log.info("Received pprof task command query for service: {}, instance: {}", 
                request.getService(), request.getServiceInstance());
        
        // TODO: fetch tasks from cache
        responseObserver.onNext(Commands.newBuilder().build());
        responseObserver.onCompleted();
        // TODO: record task log
    }

    public static void recordPprofTaskLog(PprofTask task, String instanceId, PprofTaskLogOperationType operationType) {
        // PprofTaskLogRecord logRecord = new PprofTaskLogRecord();
        // logRecord.setTaskId(task.getId());
        // logRecord.setInstanceId(instanceId);
        // logRecord.setOperationType(operationType.getCode());
        // logRecord.setTimestamp(System.currentTimeMillis());
        // logRecord.setStatus(true);
        // logRecord.setMessage("Pprof task log recorded");
    }

    public static PprofCollectionMetaData parseMetaData(PprofMetaData metaData ) throws IOException {
        String serviceId = IDManager.ServiceID.buildId(metaData.getService(), true);
        String serviceInstanceId = IDManager.ServiceInstanceID.buildId(serviceId, metaData.getServiceInstance());
        return PprofCollectionMetaData.builder()
                .task(null) // TODO: query task
                .serviceId(serviceId)
                .instanceId(serviceInstanceId)
                .type(metaData.getType().name())
                .contentSize(metaData.getContentSize())
                .uploadTime(System.currentTimeMillis())
                .build();
    }
} 