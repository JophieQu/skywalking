package org.apache.skywalking.oap.server.core.profiling.pprof.storage;

import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.record.Record;
import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;
import org.apache.skywalking.oap.server.core.source.ScopeDeclaration;
import org.apache.skywalking.oap.server.core.storage.StorageID;
import org.apache.skywalking.oap.server.core.storage.annotation.BanyanDB;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;
import org.apache.skywalking.oap.server.core.storage.annotation.ElasticSearch;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Entity;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Storage;
import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.PPROF_TASK_LOG;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ScopeDeclaration(id = PPROF_TASK_LOG, name = "PprofTaskLog")
@Stream(name = PprofTaskLogRecord.INDEX_NAME, scopeId = PPROF_TASK_LOG, builder = PprofTaskLogRecord.Builder.class, processor = RecordStreamProcessor.class)
@BanyanDB.TimestampColumn(PprofTaskLogRecord.TIMESTAMP)
public class PprofTaskLogRecord extends Record {
    public static final String INDEX_NAME = "pprof_task_log";
    public static final String TASK_ID = "task_id";
    public static final String INSTANCE_ID = "instance_id";
    public static final String OPERATION_TYPE = "operation_type";
    public static final String OPERATION_TIME = "operation_time";
    public static final String TIMESTAMP = "timestamp";

    @Column(name = TASK_ID)
    private String taskId;
    @Column(name = INSTANCE_ID)
    @BanyanDB.SeriesID(index = 0)
    private String instanceId;
    @Column(name = OPERATION_TYPE, storageOnly = true)
    private int operationType;
    @ElasticSearch.EnableDocValues
    @Column(name = OPERATION_TIME)
    private long operationTime;
    @Getter
    @Setter
    @ElasticSearch.EnableDocValues
    @Column(name = TIMESTAMP)
    private long timestamp;

    @Override
    public StorageID id() {
        return new StorageID()
                .append(TASK_ID, getTaskId())
                .append(INSTANCE_ID, getInstanceId())
                .append(OPERATION_TYPE, getOperationType())
                .append(OPERATION_TIME, getOperationTime());
    }

    public static class Builder implements StorageBuilder<PprofTaskLogRecord> {
        @Override
        public PprofTaskLogRecord storage2Entity(final Convert2Entity converter) {
            final PprofTaskLogRecord log = new PprofTaskLogRecord();
            log.setTaskId((String) converter.get(TASK_ID));
            log.setInstanceId((String) converter.get(INSTANCE_ID));
            log.setOperationType(((Number) converter.get(OPERATION_TYPE)).intValue());
            log.setOperationTime(((Number) converter.get(OPERATION_TIME)).longValue());
            log.setTimestamp(((Number) converter.get(TIMESTAMP)).longValue());
            log.setTimeBucket(((Number) converter.get(TIME_BUCKET)).longValue());
            return log;
        }

        @Override
        public void entity2Storage(final PprofTaskLogRecord storageData, final Convert2Storage converter) {
            converter.accept(TASK_ID, storageData.getTaskId());
            converter.accept(INSTANCE_ID, storageData.getInstanceId());
            converter.accept(OPERATION_TYPE, storageData.getOperationType());
            converter.accept(OPERATION_TIME, storageData.getOperationTime());
            converter.accept(TIME_BUCKET, storageData.getTimeBucket());
            converter.accept(TIMESTAMP, storageData.getTimestamp());
        }
    }
}
