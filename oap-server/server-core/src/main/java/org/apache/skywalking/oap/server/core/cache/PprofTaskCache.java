package org.apache.skywalking.oap.server.core.cache;
import org.apache.skywalking.oap.server.library.module.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.skywalking.oap.server.core.CoreModuleConfig;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;

import org.apache.skywalking.oap.server.core.query.type.PprofTask;
import org.apache.skywalking.oap.server.core.storage.StorageModule;

import org.apache.skywalking.oap.server.core.storage.profiling.pprof.IPprofTaskQueryDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PprofTaskCache implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(PprofTaskCache.class);

    private final Cache<String, PprofTask> serviceId2taskCache;
    private final ModuleManager moduleManager;

    private IPprofTaskQueryDAO taskQueryDAO;

    public PprofTaskCache(ModuleManager moduleManager, CoreModuleConfig moduleConfig) {
        this.moduleManager = moduleManager;
        long initialSize = moduleConfig.getMaxSizeOfProfileTask() / 10L;
        int initialCapacitySize = (int) (initialSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : initialSize);
    
        serviceId2taskCache = CacheBuilder.newBuilder()
                .initialCapacity(initialCapacitySize)
                .maximumSize(moduleConfig.getMaxSizeOfProfileTask())
                // remove old profile task data
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    private IPprofTaskQueryDAO getTaskQueryDAO() {
        if (Objects.isNull(taskQueryDAO)) {
            taskQueryDAO = moduleManager.find(StorageModule.NAME)
                    .provider()
                    .getService(IPprofTaskQueryDAO.class);
        }
        return taskQueryDAO;
    }

    public PprofTask getPprofTask(String serviceId) {
//        LOGGER.info("[Pprof任务缓存] 查询缓存 - serviceId={}, 长度={}", serviceId, serviceId.length());
//        LOGGER.info("[Pprof任务缓存] 当前缓存键列表: {}", serviceId2taskCache.asMap().keySet());
//
        PprofTask task = serviceId2taskCache.getIfPresent(serviceId);
        
//        if (task != null) {
//            LOGGER.info("[Pprof任务缓存] 缓存命中 - serviceId={}, taskId={}, createTime={}, events={}",
//                    serviceId, task.getId(), task.getCreateTime(), task.getEvents());
//        } else {
//            LOGGER.info("[Pprof任务缓存] 缓存未命中 - serviceId={}", serviceId);
//        }
//
        return task;
    }

    public void saveTask(String serviceId, PprofTask task) {
        if (task == null) {
            return ;
        }

        serviceId2taskCache.put(serviceId, task);
    }

    /**
     * use for every db query, -5min start time
     */
    public long getCacheStartTimeBucket() {
        return TimeBucket.getRecordTimeBucket(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
    }

    /**
     * use for every db query, +5min end time(because search through task's start time)
     */
    public long getCacheEndTimeBucket() {
        return TimeBucket.getRecordTimeBucket(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
    }

}
