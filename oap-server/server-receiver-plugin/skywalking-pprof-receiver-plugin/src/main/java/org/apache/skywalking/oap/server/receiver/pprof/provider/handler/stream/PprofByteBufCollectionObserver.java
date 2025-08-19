/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.pprof.v10.PprofData;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.apm.network.pprof.v10.PprofCollectionResponse;
import org.apache.skywalking.apm.network.pprof.v10.PprofProfilingStatus;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofTaskQueryDAO;
import java.io.IOException;

import static org.apache.skywalking.oap.server.receiver.pprof.provider.handler.PprofServiceHandler.parseMetaData;

import java.nio.ByteBuffer;
import java.util.Objects;

@Slf4j
public class PprofByteBufCollectionObserver implements StreamObserver<PprofData> {
    private final IPprofTaskQueryDAO taskDAO;
    private final StreamObserver<PprofCollectionResponse> responseObserver;
    private final SourceReceiver sourceReceiver;
    private final int pprofMaxSize;
    private PprofCollectionMetaData taskMetaData;
    private ByteBuffer buf;

    public PprofByteBufCollectionObserver(IPprofTaskQueryDAO taskDAO, 
                                         StreamObserver<PprofCollectionResponse> responseObserver, 
                                         SourceReceiver sourceReceiver, int pprofMaxSize) {
        this.taskDAO = taskDAO;
        this.responseObserver = responseObserver;
        this.sourceReceiver = sourceReceiver;
        this.pprofMaxSize = pprofMaxSize;
    }

    @Override
    @SneakyThrows
    public void onNext(PprofData pprofData) {
        if (Objects.isNull(taskMetaData) && pprofData.hasMetadata()) {
            taskMetaData = parseMetaData(pprofData.getMetadata(),taskDAO);
            
            if (PprofProfilingStatus.PPROF_PROFILING_SUCCESS.equals(taskMetaData.getType())) {
                int size = taskMetaData.getContentSize();
                if (pprofMaxSize >= size) {
                    buf = ByteBuffer.allocate(size);
                    // Send success response to allow client to continue uploading
                    responseObserver.onNext(PprofCollectionResponse.newBuilder()
                            .setStatus(PprofProfilingStatus.PPROF_PROFILING_SUCCESS)
                            .build());
                    
                    log.info("Started collecting pprof data in memory - service: {}, serviceInstance: {}, size: {} bytes", 
                            pprofData.getMetadata().getService(), pprofData.getMetadata().getServiceInstance(), size);
                } else {
                    responseObserver.onNext(PprofCollectionResponse.newBuilder()
                            .setStatus(PprofProfilingStatus.PPROF_TERMINATED_BY_OVERSIZE)
                            .build());
                    // TODO: record task oversize log
                    log.warn("Pprof file size {} exceeds maximum allowed size {} for service: {}, serviceInstance: {}",
                            size, pprofMaxSize, pprofData.getMetadata().getService(), pprofData.getMetadata().getServiceInstance());
                }
            } else {
                responseObserver.onNext(PprofCollectionResponse.newBuilder()
                        .setStatus(PprofProfilingStatus.PPROF_EXECUTION_TASK_ERROR)
                        .build());
                // TODO: record task err log
                log.error("Received execution error from agent - service: {}, serviceInstance: {}, status: {}",
                        pprofData.getMetadata().getService(), pprofData.getMetadata().getServiceInstance(), taskMetaData.getType());
            }
        } else if (pprofData.hasContent()) {
            if (buf != null) {
                pprofData.getContent().copyTo(buf);
                
                if (log.isDebugEnabled()) {
                    log.debug("Received {} bytes of pprof data", pprofData.getContent().size());
                }
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Status status = Status.fromThrowable(throwable);
        if (Status.CANCELLED.getCode() == status.getCode()) {
            if (log.isDebugEnabled()) {
                log.debug("Pprof data collection cancelled: {}", throwable.getMessage());
            }
        } else {
            log.error("Error in receiving pprof profiling data", throwable);
        }
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

    

    private void parseAndStorageData(PprofCollectionMetaData taskMetaData, ByteBuffer buf) throws IOException {
        PprofTask task = taskMetaData.getTask();
        if (task == null) {
            log.error("Pprof instanceId:{} has not been assigned a task but still uploaded data", taskMetaData.getInstanceId());
            return;
        }
        // TODO: record pprof task log
        parsePprofAndStorage(taskMetaData, buf);
    }

    public void parsePprofAndStorage(PprofCollectionMetaData taskMetaData, ByteBuffer buf) throws IOException {
        PprofTask task = taskMetaData.getTask();
        // Map<String, FrameTree> result = PprofParser.parsePprofFile(buf);
        // for (Map.Entry<String, FrameTree> entry : result.entrySet()) {
        //     String eventType = entry.getKey();
        //     FrameTree tree = entry.getValue();
        //     PprofProfilingData data = new PprofProfilingData();
        //     data.setEventType(eventType);
        //     data.setFrameTree(tree);
        //     data.setTaskId(task.getId());
        //     data.setInstanceId(metaData.getInstanceId());
        //     data.setUploadTime(metaData.getUploadTime());
        //     sourceReceiver.receive(data);
        // }
    }
}
