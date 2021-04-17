package com.arcana.fischl.auth;

import org.apache.curator.RetryPolicy;
import org.apache.curator.drivers.TracerDriver;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.cloud.zookeeper.CuratorFrameworkCustomizer;
import org.springframework.cloud.zookeeper.ZookeeperProperties;
import org.springframework.context.annotation.Bean;

@BootstrapConfiguration
public class BootstrapConfigurer {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public CuratorFramework curatorFramework(ZookeeperProperties properties, RetryPolicy retryPolicy,
                                             ObjectProvider<CuratorFrameworkCustomizer> optionalCuratorFrameworkCustomizerProvider,
                                             ObjectProvider<EnsembleProvider> optionalEnsembleProvider,
                                             ObjectProvider<TracerDriver> optionalTracerDriverProvider) throws Exception {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();

        EnsembleProvider ensembleProvider = optionalEnsembleProvider.getIfAvailable();
        if (ensembleProvider != null) {
            builder.ensembleProvider(ensembleProvider);
        } else {
            builder.connectString(properties.getConnectString());
        }
        builder.sessionTimeoutMs((int) properties.getSessionTimeout().toMillis())
                .connectionTimeoutMs((int) properties.getConnectionTimeout().toMillis())
                .retryPolicy(retryPolicy);

        optionalCuratorFrameworkCustomizerProvider.orderedStream()
                .forEach(curatorFrameworkCustomizer -> curatorFrameworkCustomizer
                        .customize(builder));
        builder.authorization("digest", "bob:bobsecret".getBytes());
        CuratorFramework curator = builder.build();
        optionalTracerDriverProvider.ifAvailable(tracerDriver -> {
            if (curator.getZookeeperClient() != null) {
                curator.getZookeeperClient().setTracerDriver(tracerDriver);
            }
        });
        curator.start();
        curator.blockUntilConnected(properties.getBlockUntilConnectedWait(),
                properties.getBlockUntilConnectedUnit());
        return curator;
    }
}
