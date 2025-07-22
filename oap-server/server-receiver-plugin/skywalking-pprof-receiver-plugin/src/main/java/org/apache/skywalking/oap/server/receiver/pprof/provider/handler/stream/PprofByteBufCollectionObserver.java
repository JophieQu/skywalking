package org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream;

import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.common.v3.Command;
import org.apache.skywalking.oap.server.library.pprof.parser.PprofParser;
import entity.FrameTree;
import java.util.Map;
import org.apache.skywalking.oap.server.core.source.PprofProfilingData;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofData;
import org.apache.skywalking.apm.network.language.pprof.v10.PprofProfilingStatus;

import static org.apache.skywalking.oap.server.receiver.pprof.provider.handler.PprofServiceHandler.parseMetaData;

import java.io.IOException;
import java.nio.ByteBuffer;

@Slf4j
public class PprofByteBufCollectionObserver implements StreamObserver<PprofData> {
    private final StreamObserver<Commands> responseObserver;
    private final SourceReceiver sourceReceiver;
    private final int pprofMaxSize;
    private PprofCollectionMetaData taskMetaData;
    private ByteBuffer buf;

    public PprofByteBufCollectionObserver(Object taskDAO, StreamObserver<Commands> responseObserver, 
                                         SourceReceiver sourceReceiver, int pprofMaxSize) {
        this.responseObserver = responseObserver;
        this.sourceReceiver = sourceReceiver;
        this.pprofMaxSize = pprofMaxSize;
    }

    @Override
    public void onNext(PprofData pprofData) {
        if (pprofData != null && pprofData.hasMetaData()) {
            try {
                taskMetaData = parseMetaData(pprofData.getMetaData());
                if(PprofProfilingStatus.PPROF_PROFILING_SUCCESS.name().equals(taskMetaData.getType())) {
                    int size = taskMetaData.getContentSize();
                    if(pprofMaxSize >= size) {
                        buf = ByteBuffer.allocate(size);
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
                log.error("Failed to parse pprof metadata", e);
                onError(e);
                return;
            }
        } else if (pprofData != null && pprofData.hasContent()) {
            // 把client的数据写入buf
            pprofData.getContent().copyTo(buf);
        }

    }

    @Override
    public void onError(Throwable t) {
        Status status = Status.fromThrowable(t);
        if (Status.CANCELLED.getCode() == status.getCode()) {
            if (log.isDebugEnabled()) {
                log.debug(t.getMessage(),t);
            }
        }
        log.error("Error occurred while receiving pprof data", t);
    }

    @Override
    @SneakyThrows
    public void onCompleted() {
        responseObserver.onCompleted();
        if (Objects.nonNull(buf)) {
            buf.flip();
            parseAndStorageData(taskMetaData, buf);
        }
    }
    private void parseAndStorageData(PprofCollectionMetaData metaData, ByteBuffer buf) throws IOException {
        PprofTask task = metaData.getTask();
        if (task == null) {
            log.error("Pprof metadata is null, cannot process pprof data");
            return;
        }

        
        // TODO: Record task log
        // recordPprofTaskLog(task, metaData.getInstanceId(), PprofTaskLogOperationType.EXECUTION_FINISHED);

        parsePprofAndStorage(metaData, buf);
    }

    public void parsePprofAndStorage(PprofCollectionMetaData metaData, 
                            ByteBuffer buf) throws IOException {
        log.info("Parsing pprof file for service: {}, instance: {}", 
                metaData.getServiceId(), metaData.getInstanceId());
        PprofTask task = metaData.getTask();
        Map<String, FrameTree> result = PprofParser.parsePprofFile(buf);
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
} 