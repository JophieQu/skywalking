package org.apache.skywalking.oap.server.receiver.pprof.module;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;

@Getter
@Setter
public class PprofModuleConfig extends ModuleConfig {
    /**
     * Used to manage the maximum size of the pprof file that can be received, the unit is Byte
     * default is 30M
     */
    private int PprofMaxSize = 30 * 1024 * 1024;
    /**
     * default is true
     * <p>
     * If memoryParserEnabled is true, then PprofByteBufCollectionObserver will be enabled
     * will use memory to receive pprof files without writing files (this is currently used).
     * This can prevent the oap server from crashing due to no volume mounting.
     * <p>
     * If memoryParserEnabled is false, then PprofFileCollectionObserver will be enabled
     * which uses createTemp to write files and then reads the files for parsing.
     * The advantage of this is that it reduces memory and prevents the oap server from crashing due to
     * insufficient memory, but it may report an error due to no volume mounting.
     */
    private boolean memoryParserEnabled = true;
}
