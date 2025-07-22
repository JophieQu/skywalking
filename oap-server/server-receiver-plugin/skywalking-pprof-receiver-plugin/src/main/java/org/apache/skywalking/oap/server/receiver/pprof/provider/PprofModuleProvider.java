package org.apache.skywalking.oap.server.receiver.pprof.provider;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.receiver.pprof.module.PprofModule;
import org.apache.skywalking.oap.server.receiver.pprof.module.PprofModuleConfig;
import org.apache.skywalking.oap.server.receiver.pprof.provider.handler.PprofServiceHandler;
import org.apache.skywalking.oap.server.receiver.sharing.server.SharingServerModule;

public class PprofModuleProvider extends ModuleProvider {
    private PprofModuleConfig config;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return PprofModule.class;
    }

    @Override
    public ConfigCreator<? extends ModuleConfig> newConfigCreator() {
        return new ConfigCreator<PprofModuleConfig>() {
            @Override
            public Class type() {
                return PprofModuleConfig.class;
            }

            @Override
            public void onInitialized(final PprofModuleConfig initialized) {
                config = initialized;
            }
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
        GRPCHandlerRegister grpcHandlerRegister = getManager().find(SharingServerModule.NAME)
                .provider()
                .getService(GRPCHandlerRegister.class);
        PprofServiceHandler pprofServiceHandler = new PprofServiceHandler(getManager(),
                config.getPprofMaxSize(), config.isMemoryParserEnabled());
        grpcHandlerRegister.addHandler(pprofServiceHandler);
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
    }

    @Override
    public String[] requiredModules() {
        return new String[]{
                CoreModule.NAME,
                SharingServerModule.NAME
        };
    }
} 