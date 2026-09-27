package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.HitRequestDto;
import ru.practicum.dto.StatsViewDto;

import java.util.List;

@Service
public class StatsClient {
    private final RestClient restClient;

    @Autowired
    public StatsClient(RestClient.Builder builder, @Value("${stats-server.url}") String serverUrl) {
        this.restClient = builder
                .baseUrl(serverUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }

    public void hit(HitRequestDto requestDto) {
        restClient.post()
                .uri("/hit")
                .body(requestDto)
                .retrieve()
                .toBodilessEntity();
    }

    public List<StatsViewDto> getStats(String start, String end, List<String> uris, boolean unique) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/stats");
                    uriBuilder.queryParam("start", start);
                    uriBuilder.queryParam("end", end);
                    uriBuilder.queryParam("unique", unique);

                    if (uris != null && !uris.isEmpty()) {
                        uriBuilder.queryParam("uris", uris.toArray());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}