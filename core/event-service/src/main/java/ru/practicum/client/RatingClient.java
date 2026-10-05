package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "rating-service")
public interface RatingClient {
    @PostMapping("/internal/ratings/events") Map<Long, Integer> getRatings(@RequestBody List<Long> eventIds);
}
