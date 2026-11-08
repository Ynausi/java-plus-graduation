package ru.practicum.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.aggregator.service.AggregationProcessor;

@SpringBootApplication
public class AggregatorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(AggregatorApplication.class, args);
        context.getBean(AggregationProcessor.class).start();
    }
}
