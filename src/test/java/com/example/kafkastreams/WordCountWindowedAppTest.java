package com.example.kafkastreams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.WindowStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class WordCountWindowedAppTest {

    private TopologyTestDriver driver;
    private TestInputTopic<String, String>  inputTopic;
    private TestOutputTopic<String, Long>   outputTopic;
    private final Instant testStart = Instant.parse("2025-05-16T12:00:00Z");   // arbitrary but fixed

    @BeforeEach
    void setup() {
        /* ---- build the topology under test ---- */
        WordCountWindowedApp app = new WordCountWindowedApp();
        Topology topology = buildTopology();

        /* ---- config identical (apart from dummy bootstrap) ---- */
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-wordcount-windowed");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        driver = new TopologyTestDriver(topology, props, testStart);

        inputTopic  = driver.createInputTopic(
                "streams-windowed-wordcount-input",
                Serdes.String().serializer(),
                Serdes.String().serializer(),
                testStart,
                Duration.ofMillis(0));

        outputTopic = driver.createOutputTopic(
                "streams-windowed-wordcount-output",
                Serdes.String().deserializer(),
                Serdes.Long().deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    /* ---------- the actual tests ---------- */

    @Test
    void countsWordsWithinOneMinuteWindow() {
        // Produce three lines into the same one-minute window
        inputTopic.pipeInput(null, "Hello Kafka Streams");
        inputTopic.pipeInput(null, "Kafka Streams is cool");
        inputTopic.pipeInput(null, "Streams process data fast");

        // Advance virtual time to let the window close & emit
        driver.advanceWallClockTime(Duration.ofMinutes(1));

        // We expect: hello=1, kafka=2, streams=3, …
        assertThat(outputTopic.readKeyValuesToMap())
                .containsEntry("hello",   1L)
                .containsEntry("kafka",   2L)
                .containsEntry("streams", 3L)
                .containsEntry("cool",    1L);
    }

    /* ---------- helper to build the same topology as production ---------- */

    private Topology buildTopology() {
        // This mirrors exactly what WordCountWindowedApp.main() builds
        StreamsBuilder builder = new StreamsBuilder();

        TimeWindows window = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1));

        builder.<String, String>stream("streams-windowed-wordcount-input")
                .flatMapValues(value -> List.of(value.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .windowedBy(window)
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>
                        as("windowed-counts-store"))
                .toStream()
                .map((windowedWord, count) -> KeyValue.pair(windowedWord.key(), count))
                .to("streams-windowed-wordcount-output",
                        Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}
