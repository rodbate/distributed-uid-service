package com.github.rodbate.uid.web.config;

import com.github.rodbate.uid.common.ImmutableConfig;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.jmx.JmxMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;

/**
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 11:46
 */
@Slf4j
public class BootApplicationRunListener implements SpringApplicationRunListener {

    private final WeakReference<SpringApplication> springApplicationRef;
    private final String[] args;


    public BootApplicationRunListener(SpringApplication application, String[] args) {
        this.springApplicationRef = new WeakReference<>(application);
        this.args = args;
    }

    @Override
    public void starting() {
        log.info("Application Starting");
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        ImmutableConfig.processConfig(environment);
        log.info("Application EnvironmentPrepared => Application Env: {}, command args: {}", ImmutableConfig.appEnv.getEnv(), Arrays.toString(this.args));
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        log.info("Application ContextPrepared");
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        log.info("Application ContextLoaded");
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        log.info("Application Started");
        JmxMeterRegistry jmxMeterRegistry = Objects.requireNonNull(context.getBean(JmxMeterRegistry.class));
        jmxMeterRegistry.start();
        Metrics.addRegistry(jmxMeterRegistry);
        log.info("JmxMeterRegistry started");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        log.info("Application Running");
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        log.error("Application Failed", exception);
    }


}
