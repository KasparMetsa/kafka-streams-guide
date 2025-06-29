package com.example.kafkastreams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class ParallelStreamsApp {

    public static void main(String[] args) {
        // First pipeline for parallel-topic1 (user data)
        Thread userPipeline = new Thread(() -> startStreamPipeline(
                "user-pipeline-app",
                "parallel-topic1",
                "processed-users-topic"
        ));

        // Second pipeline for parallel-topic2 (order data)
        Thread orderPipeline = new Thread(() -> startStreamPipeline(
                "order-pipeline-app",
                "parallel-topic2",
                "processed-orders-topic"
        ));

        userPipeline.start();
        orderPipeline.start();
    }

    private static void startStreamPipeline(String applicationId, String inputTopic, String outputTopic) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId); // 👈 Unique app ID
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();

        // Simple stream processing: log and forward
        KStream<String, String> stream = builder.stream(inputTopic);
        stream.peek((key, value) -> System.out.printf("App: %s | Read record from %s -> key: %s, value: %s%n", applicationId, inputTopic, key, value))
                .to(outputTopic);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }
}
