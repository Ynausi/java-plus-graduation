package ru.practicum.aggregator.config;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EurekaRegistration {

    private final EurekaClient eurekaClient;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        eurekaClient.getApplicationInfoManager()
                .setInstanceStatus(InstanceInfo.InstanceStatus.UP);

        log.info("Aggregator marked UP in Eureka");
    }
}
