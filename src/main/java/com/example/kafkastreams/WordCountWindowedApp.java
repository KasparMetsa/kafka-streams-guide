package com.example.kafkastreams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.kstream.TimeWindows;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class WordCountWindowedApp {

    private static final String INPUT_TOPIC  = "streams-windowed-wordcount-input";
    private static final String OUTPUT_TOPIC = "streams-windowed-wordcount-output";

    public static void main(String[] args) {

        /* ---- basic config ---- */
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-windowed-application");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /* ---- topology ---- */
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> textLines = builder.stream(INPUT_TOPIC);

        TimeWindows window = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1));

        KTable<Windowed<String>, Long> wordCounts = textLines
                .flatMapValues(line -> Arrays.asList(line.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)         // key is the word
                .windowedBy(window)                   // <----- one-minute tumbling windows
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>
                        as("windowed-counts-store"));

        wordCounts
                .toStream()
                // map the Windowed<String> key to just the word for easier viewing
                .map((windowedWord, count) ->
                        KeyValue.pair(windowedWord.key(), count))
                .to(OUTPUT_TOPIC,
                        Produced.with(Serdes.String(), Serdes.Long()));

        /* ---- start ---- */
        Topology topology = builder.build();
        System.out.println(topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }
}
