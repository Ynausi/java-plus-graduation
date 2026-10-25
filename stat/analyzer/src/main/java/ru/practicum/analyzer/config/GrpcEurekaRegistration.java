package ru.practicum.analyzer.config;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcEurekaRegistration {

    private final EurekaInstanceConfigBean eurekaInstanceConfig;
    private final EurekaClient eurekaClient;

    @EventListener
    public void onGrpcServerStarted(GrpcServerStartedEvent event) {
        String port = String.valueOf(event.getPort());

        eurekaInstanceConfig.getMetadataMap().put("gRPC_port", port);
        eurekaInstanceConfig.getMetadataMap().put("grpcPort", port);
        eurekaClient.getApplicationInfoManager()
                .registerAppMetadata(eurekaInstanceConfig.getMetadataMap());
        eurekaClient.getApplicationInfoManager()
                .setInstanceStatus(InstanceInfo.InstanceStatus.UP);

        log.info("Published gRPC port {} to Eureka metadata and marked service UP", port);
    }
}
