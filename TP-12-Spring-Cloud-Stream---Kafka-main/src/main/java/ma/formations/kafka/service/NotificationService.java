package ma.formations.kafka.service;

import ma.formations.kafka.dtos.Notification;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class NotificationService {

    @Bean
    public Consumer<Notification> notificationConsumer() {
        return (notification) -> {
            System.out.println("**********************");
            System.out.println(notification);
            System.out.println("**********************");
        };
    }

    @Bean
    public Supplier<Notification> notificationSupplier() {
        List<String> matricules = List.of(
                "A-2 5643", "A-6 9876", "A-45 6549", "A-3 785", "A-2 5643"
        );

        return () -> Notification.builder()
                .registrationNumber(matricules.get(new Random().nextInt(matricules.size())))
                .date(new Date())
                .code(UUID.randomUUID().toString())
                .authorizedSpeed(Math.random() > 60 ? 60 : Math.random())
                .currentSpeed(Math.random() > 70 ? Math.random() : 100)
                .build();
    }

    @Bean
    public Function<Notification, Map<String, String>> notificationFunction() {
        return input -> {
            HashMap<String, String> map = new HashMap<>();
            map.put(
                    input.getRegistrationNumber(),
                    "CS=" + input.getCurrentSpeed()
                            + ", AS=" + input.getAuthorizedSpeed()
                            + " at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(input.getDate())
            );
            return map;
        };
    }

    @Bean
    public Function<KStream<String, Notification>, KStream<String, Long>> kStreamFunction() {
        return input -> input
                .filter((k, v) -> v != null && "A-2 5643".equals(v.getRegistrationNumber()))
                .map((k, v) -> new KeyValue<>(v.getRegistrationNumber(), 0L))
                .groupBy((k, v) -> k, Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
                .count(Materialized.as("counts"))
                .toStream()
                .map((k, v) -> new KeyValue<>(
                        "=>" + k.window().startTime() + k.window().endTime() + ":" + k.key(),
                        v
                ));
    }
}
