package org.apache.skywalking.oap.server.core.storage.profiling.pprof;
import org.apache.skywalking.oap.server.core.storage.DAO;
import org.apache.skywalking.oap.server.core.query.type.PprofTask;

import java.io.IOException;
import java.util.List;

public interface IPprofTaskQueryDAO extends DAO {

    /**
     * search task list in appoint time bucket
     *
     * @param serviceId       monitor service id, maybe null
     * @param startTimeBucket time bucket bigger than or equals, nullable
     * @param endTimeBucket   time bucket smaller than or equals, nullable
     * @param limit           limit count, if null means query all
     */
    List<PprofTask> getTaskList(final String serviceId, final Long startTimeBucket,
                                        final Long endTimeBucket, final Integer limit) throws IOException;

    /**
     * query profile task by id
     *
     * @param id taskId
     * @return task data
     */
    PprofTask getById(final String id) throws IOException;

}
