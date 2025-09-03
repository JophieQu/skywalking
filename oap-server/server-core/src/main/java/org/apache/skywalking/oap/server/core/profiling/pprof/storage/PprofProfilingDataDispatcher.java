package org.apache.skywalking.oap.server.core.profiling.pprof.storage;

import com.google.gson.Gson;

import org.apache.skywalking.oap.server.core.analysis.SourceDispatcher;
import org.apache.skywalking.oap.server.core.source.PprofProfilingData;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;
public class PprofProfilingDataDispatcher implements SourceDispatcher<PprofProfilingData> {
        private static final Gson GSON = new Gson();

        @Override
        public void dispatch(PprofProfilingData source) {
                PprofProfilingDataRecord record = new PprofProfilingDataRecord();
                record.setTaskId(source.getTaskId());
                record.setInstanceId(source.getInstanceId());
                record.setEventType(source.getEventType().toString());
                record.setDataBinary(GSON.toJson(source.getFrameTree()).getBytes());
                record.setUploadTime(source.getUploadTime());
                record.setTimeBucket(TimeBucket.getRecordTimeBucket(source.getUploadTime()));
                RecordStreamProcessor.getInstance().in(record);
        }
}
