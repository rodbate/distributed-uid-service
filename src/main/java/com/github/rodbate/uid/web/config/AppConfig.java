package com.github.rodbate.uid.web.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.github.rodbate.uid.common.Constants;
import com.github.rodbate.uid.metric.GlobalMetrics;
import com.github.rodbate.uid.web.service.IdGeneratorService;
import com.github.rodbate.uid.web.service.impl.EsIdGeneratorService;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * User: jiangsongsong
 * Date: 2018/12/26
 * Time: 15:29
 */
@Configuration
public class AppConfig {

    /**
     * reactive string redis template
     *
     * @param reactiveRedisConnectionFactory connection factory
     * @return redis template
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        return new ReactiveStringRedisTemplate(reactiveRedisConnectionFactory);
    }


    /**
     * jmx meter registry bean
     *
     * @return jmx meter registry bean
     */
    @Bean(destroyMethod = "close")
    public JmxMeterRegistry jmxMeterRegistry() {
        MetricRegistry metricRegistry = new MetricRegistry();
        return new JmxMeterRegistry(
            JmxConfig.DEFAULT,
            Clock.SYSTEM,
            HierarchicalNameMapper.DEFAULT,
            metricRegistry,
            JmxReporter.forRegistry(metricRegistry).inDomain(JmxConfig.DEFAULT.domain()).filter((name, metric) -> GlobalMetrics.isExportedToJmx(name)).build()
        );
    }

    /**
     * reactor scheduler
     *
     * @return scheduler
     */
    @Bean(initMethod = "start", destroyMethod = "dispose")
    public Scheduler reactorServiceScheduler() {
        int threadNum = Math.max(16, Constants.CPU_CORE_NUM * 2);
        return Schedulers.newParallel("reactor-service-scheduler", threadNum);
    }

    /**
     * id generator service
     *
     * @return IdGeneratorService
     */
    @Bean
    public IdGeneratorService idGeneratorService() {
        return new EsIdGeneratorService(jmxMeterRegistry());
    }

}
