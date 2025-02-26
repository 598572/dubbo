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
 */
package org.apache.dubbo.config.context;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;
import static org.apache.dubbo.config.context.ConfigManager.DUBBO_CONFIG_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link ConfigManager} Test
 *
 * @since 2.7.5
 */
public class ConfigManagerTest {

    private ConfigManager configManager;
    private ModuleConfigManager moduleConfigManager;

    @BeforeEach
    public void init() {
        ApplicationModel.defaultModel().destroy();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        configManager = applicationModel.getApplicationConfigManager();
        moduleConfigManager = applicationModel.getDefaultModule().getConfigManager();
    }

    @Test
    public void testDestroy() {
        assertTrue(configManager.configsCache.isEmpty());
    }

    @Test
    public void testDefaultValues() {
        // assert single
        assertFalse(configManager.getApplication().isPresent());
        assertFalse(configManager.getMonitor().isPresent());
        assertFalse(configManager.getMetrics().isPresent());

        // protocols
        assertTrue(configManager.getProtocols().isEmpty());
        assertTrue(configManager.getDefaultProtocols().isEmpty());

        // registries
        assertTrue(configManager.getRegistries().isEmpty());
        assertTrue(configManager.getDefaultRegistries().isEmpty());

        // config centers
        assertTrue(configManager.getConfigCenters().isEmpty());

        // metadata
        assertTrue(configManager.getMetadataConfigs().isEmpty());

        // services and references
        assertTrue(moduleConfigManager.getServices().isEmpty());
        assertTrue(moduleConfigManager.getReferences().isEmpty());

        // providers and consumers
        assertFalse(moduleConfigManager.getModule().isPresent());
        assertFalse(moduleConfigManager.getDefaultProvider().isPresent());
        assertFalse(moduleConfigManager.getDefaultConsumer().isPresent());
        assertTrue(moduleConfigManager.getProviders().isEmpty());
        assertTrue(moduleConfigManager.getConsumers().isEmpty());
    }

    // Test ApplicationConfig correlative methods
    @Test
    public void testApplicationConfig() {
        ApplicationConfig config = new ApplicationConfig("ConfigManagerTest");
        configManager.setApplication(config);
        assertTrue(configManager.getApplication().isPresent());
        assertEquals(config, configManager.getApplication().get());
    }

    // Test MonitorConfig correlative methods
    @Test
    public void testMonitorConfig() {
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setGroup("test");
        configManager.setMonitor(monitorConfig);
        assertTrue(configManager.getMonitor().isPresent());
        assertEquals(monitorConfig, configManager.getMonitor().get());
    }

    // Test MonitorConfig correlative methods
    @Test
    public void tesModuleConfig() {
        ModuleConfig config = new ModuleConfig();
        moduleConfigManager.setModule(config);
        assertTrue(moduleConfigManager.getModule().isPresent());
        assertEquals(config, moduleConfigManager.getModule().get());
    }

    // Test MetricsConfig correlative methods
    @Test
    public void testMetricsConfig() {
        MetricsConfig config = new MetricsConfig();
        config.setProtocol(PROTOCOL_PROMETHEUS);
        configManager.setMetrics(config);
        assertTrue(configManager.getMetrics().isPresent());
        assertEquals(config, configManager.getMetrics().get());
    }

    // Test ProviderConfig correlative methods
    @Test
    public void testProviderConfig() {
        ProviderConfig config = new ProviderConfig();
        moduleConfigManager.addProviders(asList(config, null));
        Collection<ProviderConfig> configs = moduleConfigManager.getProviders();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertTrue(moduleConfigManager.getDefaultProvider().isPresent());

        config = new ProviderConfig();
        config.setId(DEFAULT_KEY);
        config.setQueues(10);
        moduleConfigManager.addProvider(config);
        assertTrue(moduleConfigManager.getDefaultProvider().isPresent());
        configs = moduleConfigManager.getProviders();
        assertEquals(2, configs.size());
    }

    // Test ConsumerConfig correlative methods
    @Test
    public void testConsumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        moduleConfigManager.addConsumers(asList(config, null));
        Collection<ConsumerConfig> configs = moduleConfigManager.getConsumers();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertTrue(moduleConfigManager.getDefaultConsumer().isPresent());

        config = new ConsumerConfig();
        config.setId(DEFAULT_KEY);
        config.setThreads(10);
        moduleConfigManager.addConsumer(config);
        assertTrue(moduleConfigManager.getDefaultConsumer().isPresent());
        configs = moduleConfigManager.getConsumers();
        assertEquals(2, configs.size());
    }

    // Test ProtocolConfig correlative methods
    @Test
    public void testProtocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        configManager.addProtocols(asList(config, null));
        Collection<ProtocolConfig> configs = configManager.getProtocols();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertFalse(configManager.getDefaultProtocols().isEmpty());
    }

    // Test RegistryConfig correlative methods
    @Test
    public void testRegistryConfig() {
        RegistryConfig config = new RegistryConfig();
        configManager.addRegistries(asList(config, null));
        Collection<RegistryConfig> configs = configManager.getRegistries();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());
        assertFalse(configManager.getDefaultRegistries().isEmpty());
    }

    // Test ConfigCenterConfig correlative methods
    @Test
    public void testConfigCenterConfig() {
        String address = "zookeeper://127.0.0.1:2181";
        ConfigCenterConfig config = new ConfigCenterConfig();
        config.setAddress(address);
        configManager.addConfigCenters(asList(config, null));
        Collection<ConfigCenterConfig> configs = configManager.getConfigCenters();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());

        // add duplicated config, expecting ignore equivalent configs
        ConfigCenterConfig config2 = new ConfigCenterConfig();
        config2.setAddress(address);
        configManager.addConfigCenter(config2);

        configs = configManager.getConfigCenters();
        assertEquals(1, configs.size());
        assertEquals(config, configs.iterator().next());

    }

    @Test
    public void testAddConfig() {
        configManager.addConfig(new ApplicationConfig("ConfigManagerTest"));
        configManager.addConfig(new ProtocolConfig());
        moduleConfigManager.addConfig(new ProviderConfig());

        assertTrue(configManager.getApplication().isPresent());
        assertFalse(configManager.getProtocols().isEmpty());
        assertFalse(moduleConfigManager.getProviders().isEmpty());
    }

    @Test
    public void testRefreshAll() {
        configManager.refreshAll();
    }

    @Test
    public void testDefaultConfig() {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setDefault(false);
        assertFalse(ConfigManager.isDefaultConfig(providerConfig));

        ProviderConfig providerConfig1 = new ProviderConfig();
        assertNull(ConfigManager.isDefaultConfig(providerConfig1));

        ProviderConfig providerConfig3 = new ProviderConfig();
        providerConfig3.setDefault(true);
        assertTrue(ConfigManager.isDefaultConfig(providerConfig3));

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setDefault(false);
        assertFalse(ConfigManager.isDefaultConfig(protocolConfig));
    }

    @Test
    public void testConfigMode() {
        ApplicationConfig applicationConfig1 = new ApplicationConfig("app1");
        ApplicationConfig applicationConfig2 = new ApplicationConfig("app2");

        try {
            // test strict mode
            ApplicationModel.reset();
            ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.STRICT, configManager.getConfigMode());

            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.STRICT.name());
            ApplicationModel.reset();
            Assertions.assertEquals(ConfigMode.STRICT, configManager.getConfigMode());

            configManager.addConfig(applicationConfig1);
            try {
                configManager.addConfig(applicationConfig2);
                fail("strict mode cannot add two application configs");
            } catch (Exception e) {
                assertEquals(IllegalStateException.class, e.getClass());
                assertTrue(e.getMessage().contains("please remove redundant configs and keep only one"));
            }

            // test override mode
            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.OVERRIDE.name());
            ApplicationModel.reset();
            configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.OVERRIDE, configManager.getConfigMode());

            configManager.addConfig(applicationConfig1);
            configManager.addConfig(applicationConfig2);
            assertEquals(applicationConfig2, configManager.getApplicationOrElseThrow());


            // test ignore mode
            System.setProperty(DUBBO_CONFIG_MODE, ConfigMode.IGNORE.name());
            ApplicationModel.reset();
            configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            Assertions.assertEquals(ConfigMode.IGNORE, configManager.getConfigMode());

            configManager.addConfig(applicationConfig1);
            configManager.addConfig(applicationConfig2);
            assertEquals(applicationConfig1, configManager.getApplicationOrElseThrow());
        } finally {
            System.clearProperty(DUBBO_CONFIG_MODE);
        }
    }
}
