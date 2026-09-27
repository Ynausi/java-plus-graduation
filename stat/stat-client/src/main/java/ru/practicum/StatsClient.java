package ru.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.HitRequestDto;
import ru.practicum.dto.StatsViewDto;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsClient {
    private final RestClient.Builder restClientBuilder;
    private final DiscoveryClient discoveryClient;

    private ServiceInstance getStatServerInstance() {
        return discoveryClient
                .getInstances("stat-server")
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("stat-server not found"));
    }

    private URI getStatServerUri(String path) {
        return UriComponentsBuilder
                .fromUri(getStatServerInstance().getUri())
                .path(path)
                .build()
                .toUri();
    }

    public void hit(HitRequestDto requestDto) {
        URI uri = getStatServerUri("/hit");
        restClientBuilder
                .build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDto)
                .retrieve()
                .toBodilessEntity();
    }

    public List<StatsViewDto> getStats(String start, String end, List<String> uris, boolean unique) {
        ServiceInstance instance = getStatServerInstance();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUri(instance.getUri())
                .path("/stats")
                .queryParam("start",start)
                .queryParam("end",end)
                .queryParam("unique",unique);

        if (uris != null && !uris.isEmpty()) {
            uriBuilder.queryParam("uris",uris);
        }

        return restClientBuilder.build()
                .get()
                .uri(uriBuilder.build().encode().toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<List<StatsViewDto>>() {
                });

    }
}