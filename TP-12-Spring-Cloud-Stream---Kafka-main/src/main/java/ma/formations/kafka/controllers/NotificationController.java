package ma.formations.kafka.controllers;

import ma.formations.kafka.dtos.Notification;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
public class NotificationController {

    private final StreamBridge streamBridge;
    private final InteractiveQueryService interactiveQueryService;

    public NotificationController(StreamBridge streamBridge, InteractiveQueryService interactiveQueryService) {
        this.streamBridge = streamBridge;
        this.interactiveQueryService = interactiveQueryService;
    }

    @GetMapping("/publish/{topic}/{registrationNumber}")
    public Notification publish(@PathVariable String topic, @PathVariable String registrationNumber) {
        Notification notification = Notification.builder()
                .registrationNumber(registrationNumber)
                .date(new Date())
                .code(UUID.randomUUID().toString())
                .authorizedSpeed(60d)
                .currentSpeed(Math.random() <= 60 ? 70 : Math.random())
                .build();

        streamBridge.send(topic, notification);
        return notification;
    }

    @GetMapping(path = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> {
                    Map<String, Long> result = new HashMap<>();

                    ReadOnlyWindowStore<String, Long> windowStore =
                            interactiveQueryService.getQueryableStore("counts", QueryableStoreTypes.windowStore());

                    Instant now = Instant.now();
                    Instant from = now.minusMillis(5000);

                    KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now);
                    while (fetchAll.hasNext()) {
                        KeyValue<Windowed<String>, Long> next = fetchAll.next();
                        result.put(next.key.key(), next.value);
                    }
                    return result;
                })
                .share();
    }
}
