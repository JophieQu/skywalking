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

package org.apache.skywalking.oap.server.receiver.pprof.provider.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;

import org.apache.skywalking.apm.network.pprof.v10.PprofData;
import org.apache.skywalking.apm.network.pprof.v10.PprofTaskGrpc;
import org.apache.skywalking.apm.network.pprof.v10.PprofCollectionResponse;
import org.apache.skywalking.apm.network.pprof.v10.PprofMetaData;

import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.pprof.v10.PprofTaskCommandQuery;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.command.CommandService;
import org.apache.skywalking.oap.server.core.query.type.PprofEventType;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofTaskQueryDAO;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
//import org.apache.skywalking.oap.server.core.cache.pprofTaskCache;

//import org.apache.skywalking.oap.server.core.storage.StorageModule;
//import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofTaskQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.network.trace.component.command.PprofTaskCommand;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream.PprofByteBufCollectionObserver;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream.PprofCollectionMetaData;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.stream.PprofFileCollectionObserver;

@Slf4j
public class PprofServiceHandler extends PprofTaskGrpc.PprofTaskImplBase implements GRPCHandler {

    private final IPprofTaskQueryDAO taskDAO;
    private final CommandService commandService;
    private final SourceReceiver sourceReceiver;
    private final int PprofMaxSize;
    private final String pprofStorageDir;
    private final boolean memoryParserEnabled;

    public PprofServiceHandler(ModuleManager moduleManager, int PprofMaxSize, boolean memoryParserEnabled) {
        this.taskDAO = moduleManager.find(StorageModule.NAME).provider().getService(IPprofTaskQueryDAO.class);
        this.commandService = moduleManager.find(CoreModule.NAME).provider().getService(CommandService.class);
        this.sourceReceiver = moduleManager.find(CoreModule.NAME).provider().getService(SourceReceiver.class);
        this.PprofMaxSize = PprofMaxSize;
        this.memoryParserEnabled = memoryParserEnabled;
        // Set pprof storage directory
        this.pprofStorageDir = "/Users/jingyiqu/ospp/grpctest";
        initStorageDirectory();
    }
    
    /**
     * Initialize the storage directory for pprof files
     */
    private void initStorageDirectory() {
        try {
            Path storagePath = Paths.get(pprofStorageDir);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                log.info("Created pprof storage directory: {}", pprofStorageDir);
            }
        } catch (IOException e) {
            log.error("Failed to create pprof storage directory: {}", pprofStorageDir, e);
        }
    }
    

    
    @Override
    public StreamObserver<PprofData> collect(StreamObserver<PprofCollectionResponse> responseObserver) {
        return memoryParserEnabled ?
                new PprofByteBufCollectionObserver(taskDAO,responseObserver, sourceReceiver, PprofMaxSize)
                : new PprofFileCollectionObserver(taskDAO, responseObserver, sourceReceiver, PprofMaxSize);
    }

    

    @Override
    public void getPprofTaskCommands(PprofTaskCommandQuery request, StreamObserver<Commands> responseObserver) {
        if (log.isDebugEnabled()) {
            log.debug("Received getPprofTaskCommands request from service: {}, serviceInstance: {}, lastCommandTime: {}",
                    request.getService(), request.getServiceInstance(), request.getLastCommandTime());
        }

        String serviceId = IDManager.ServiceID.buildId(request.getService(), true);
        String serviceInstanceId = IDManager.ServiceInstanceID.buildId(serviceId, request.getServiceInstance());
        
        // TODO: fetch tasks from cache
        PprofTask task = createSamplePprofTask(serviceId);
        
        // Check if the task should be sent based on lastCommandTime
        if (Objects.isNull(task) || task.getCreateTime() <= request.getLastCommandTime() ||
                (!CollectionUtils.isEmpty(task.getServiceInstanceIds()) && !task.getServiceInstanceIds().contains(serviceInstanceId))) {
            responseObserver.onNext(Commands.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }

        PprofTaskCommand taskCommand = commandService.newPprofTaskCommand(task);
        Commands commands = Commands.newBuilder().addCommands(taskCommand.serialize()).build();
        responseObserver.onNext(commands);
        responseObserver.onCompleted();
        // todo record pprof task log
        return;
    }

    private PprofTask createSamplePprofTask(String serviceId) {
        long currentTime = System.currentTimeMillis();
        return PprofTask.builder()
                .id("pprof-task-" + currentTime)
                .serviceId(serviceId)
                .serviceInstanceIds("instance-1")
                .createTime(currentTime)
                .startTime(currentTime + 30000) // Start in 30 seconds
                .events("CPU")
                .duration(5) // 1 minutes
                .dumpPeriod(100)
                .build();
    }

    public static PprofCollectionMetaData parseMetaData(PprofMetaData metaData, IPprofTaskQueryDAO taskDAO) throws IOException {
        String taskId = metaData.getTaskId();
        PprofTask task = taskDAO.getById(taskId);
        String serviceId = IDManager.ServiceID.buildId(metaData.getService(), true);
        String serviceInstanceId = IDManager.ServiceInstanceID.buildId(serviceId, metaData.getServiceInstance());
        
        return PprofCollectionMetaData.builder()
                .task(task)
                .serviceId(serviceId)
                .instanceId(serviceInstanceId)
                .type(metaData.getType())
                .contentSize(metaData.getContentSize())
                .uploadTime(System.currentTimeMillis())
                .build();
    }
}