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

package org.apache.skywalking.oap.server.network.trace.component.command;

import org.apache.skywalking.apm.network.common.v3.Command;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import java.util.List;
import lombok.Getter;
import java.util.Objects;

@Getter
public class PprofTaskCommand extends BaseCommand implements Serializable, Deserializable<PprofTaskCommand> {
    public static final Deserializable<PprofTaskCommand> DESERIALIZER = new PprofTaskCommand("", "", 0, 0, 0, 0);
    public static final String NAME = "PprofTaskQuery";
    // Task ID uniquely identifies a profiling task
    private String taskId;
    // Type of profiling (CPU/Alloc/Block/Mutex)  
    private String events;
    // unit is minute
    private long duration;
    // Unix timestamp in milliseconds when the task should start
    private long startTime;
    // Unix timestamp in milliseconds when the task was created
    private long createTime;
    // unit is hz
    private int dumpPeriod;

    public PprofTaskCommand(String serialNumber, String taskId, List<String> events,
                            long duration, long startTime, long createTime, int dumpPeriod) {
        super(NAME, serialNumber);
        this.taskId = taskId;
        this.duration = duration;
        this.startTime = startTime;
        this.createTime = createTime;
        String comma = ",";
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(events) && !events.isEmpty()) {
            sb.append("event=").append(String.join(comma, events));
        }
        this.dumpPeriod = dumpPeriod;
    }
    
    public PprofTaskCommand(String serialNumber, String taskId,  
                            long duration, long startTime, long createTime, int dumpPeriod) {
        super(NAME, serialNumber);
        this.taskId = taskId;
        this.duration = duration;
        this.startTime = startTime;
        this.createTime = createTime;
        this.dumpPeriod = dumpPeriod;
    }

    @Override
    public PprofTaskCommand deserialize(Command command) {
        final List<KeyStringValuePair> argsList = command.getArgsList();
        String taskId = null;
        String profileType = null;
        long duration = 0;
        long startTime = 0;
        long createTime = 0;
        int dumpPeriod = 0;
        String serialNumber = null;
        for (final KeyStringValuePair pair : argsList) {
            if ("SerialNumber".equals(pair.getKey())) {
                serialNumber = pair.getValue();
            } else if ("TaskId".equals(pair.getKey())) {
                taskId = pair.getValue();
            } else if ("ProfileType".equals(pair.getKey())) {
                profileType = pair.getValue();
            } else if ("Duration".equals(pair.getKey())) {
                duration = Long.parseLong(pair.getValue());
            } else if ("StartTime".equals(pair.getKey())) {
                startTime = Long.parseLong(pair.getValue());
            } else if ("CreateTime".equals(pair.getKey())) {
                createTime = Long.parseLong(pair.getValue());
            } else if ("DumpPeriod".equals(pair.getKey())) {
                dumpPeriod = Integer.parseInt(pair.getValue());
            }
        }
        return new PprofTaskCommand(serialNumber, taskId, duration, startTime, createTime, dumpPeriod);
    }

    @Override
    public Command.Builder serialize() {
        final Command.Builder builder = commandBuilder();
        builder.addArgs(KeyStringValuePair.newBuilder().setKey("TaskId").setValue(taskId))
                .addArgs(KeyStringValuePair.newBuilder().setKey("ProfileType").setValue(events))
                .addArgs(KeyStringValuePair.newBuilder().setKey("Duration").setValue(String.valueOf(duration)))
                .addArgs(KeyStringValuePair.newBuilder().setKey("StartTime").setValue(String.valueOf(startTime)))
                .addArgs(KeyStringValuePair.newBuilder().setKey("CreateTime").setValue(String.valueOf(createTime)))
                .addArgs(KeyStringValuePair.newBuilder().setKey("DumpPeriod").setValue(String.valueOf(dumpPeriod)));
        return builder;
    }
}
