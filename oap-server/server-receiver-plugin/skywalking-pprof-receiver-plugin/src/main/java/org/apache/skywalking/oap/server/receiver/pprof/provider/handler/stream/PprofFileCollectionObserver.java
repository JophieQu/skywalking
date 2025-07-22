package org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.common.v3.Command;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofData;
import org.apache.skywalking.oap.server.core.source.PprofProfilingData;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofProfilingStatus;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.PprofServiceHandler;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.oap.server.library.pprof.parser.PprofParser;
import entity.FrameTree;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
public class PprofFileCollectionObserver implements StreamObserver<PprofData> {
    private final StreamObserver<Commands> responseObserver;
    private final SourceReceiver sourceReceiver;
    private final int pprofMaxSize;
    private PprofCollectionMetaData taskMetaData;
    private File tempFile;
    private FileOutputStream fileOutputStream;

    public PprofFileCollectionObserver(Object taskDAO, StreamObserver<Commands> responseObserver, 
                                      SourceReceiver sourceReceiver, int pprofMaxSize) {
        this.responseObserver = responseObserver;
        this.sourceReceiver = sourceReceiver;
        this.pprofMaxSize = pprofMaxSize;
    }

    @Override
    public void onNext(PprofData pprofData) {
        if (Objects.isNull(taskMetaData) && pprofData.hasMetaData()) {
            try {
                taskMetaData = PprofServiceHandler.parseMetaData(pprofData.getMetaData());
                PprofTask task = taskMetaData.getTask();
                if(PprofProfilingStatus.PPROF_PROFILING_SUCCESS.name().equals(taskMetaData.getType())) {
                    int size = taskMetaData.getContentSize();
                    if(pprofMaxSize >= size) {
                        tempFile = File.createTempFile(task.getId() + taskMetaData.getInstanceId() + System.currentTimeMillis(), ".pprof");
                        responseObserver.onNext(Commands.newBuilder().build());
                    } else {
                        responseObserver.onNext(Commands.newBuilder()
                        .addCommands(Command.newBuilder()
                            .setCommand(PprofProfilingStatus.PPROF_TERMINATED_BY_OVERSIZE.name())
                            .build())
                        .build());
                        // TODO: record task err
                    }
                } else {
                    // TODO: record task err
                }
            } catch (IOException e) {
                log.error("Failed to parse metadata or create temp file", e);
                onError(e);
                return;
            }
        } else if (pprofData != null && pprofData.hasContent()) {
            try {
                fileOutputStream.write(pprofData.getContent().toByteArray());
            } catch (IOException e) {
                log.error("Failed to write pprof data", e);
                onError(e);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error occurred while receiving pprof data", t);
        cleanup();
        responseObserver.onError(t);
    }

    @Override
    public void onCompleted() {
        responseObserver.onCompleted();
        if (Objects.nonNull(tempFile)) {
            try {
                fileOutputStream.close();
                parseAndStorageData(taskMetaData, tempFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to close file or parse data", e);
            } finally {
                if(!tempFile.delete()) {
                    log.warn("Failed to delete tmp pprof file");
                }
            }
        }
    }

    private void parseAndStorageData(PprofCollectionMetaData metaData, String fileName) throws IOException {
        PprofTask task = metaData.getTask();
        if (task == null) {
            log.error("Pprof metadata is null, cannot process pprof data");
            return;
        }

        log.info("Starting to parse pprof file: {}", fileName);
        
        // TODO: Record task log
        // recordPprofTaskLog(task, metaData.getInstanceId(), PprofTaskLogOperationType.EXECUTION_FINISHED);

        parsePprofAndStorage(metaData, fileName);
    }

    public void parsePprofAndStorage(PprofCollectionMetaData metaData, 
                            String fileName) throws IOException {
        log.info("Parsing pprof file for service: {}, instance: {}", 
                metaData.getServiceId(), metaData.getInstanceId());
        PprofTask task = metaData.getTask();
        Map<String, FrameTree> result = PprofParser.parsePprofFile(fileName);
        for (Map.Entry<String, FrameTree> entry : result.entrySet()) {
            String eventType = entry.getKey();
            FrameTree tree = entry.getValue();
            PprofProfilingData data = new PprofProfilingData();
            data.setEventType(eventType);
            data.setFrameTree(tree);
            data.setTaskId(task.getId());
            data.setInstanceId(metaData.getInstanceId());
            data.setUploadTime(metaData.getUploadTime());
            sourceReceiver.receive(data);
        }
    }

    private void cleanup() {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                log.warn("Failed to close file output stream", e);
            }
        }
        
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                log.warn("Failed to delete temporary pprof file: {}", tempFile.getAbsolutePath());
            }
        }
    }
} 