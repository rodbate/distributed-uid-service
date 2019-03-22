package com.github.rodbate.uid.common.web;

import com.github.rodbate.uid.common.ImmutableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * User: jiangsongsong
 * Date: 2019/1/5
 * Time: 11:08
 */
@Slf4j
@Component
public class ApplicationContextClosedListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("context closing , {}", event);
        ImmutableConfig.applicationClosed = true;
    }

}
