package connect;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class SinkToProducerForwarderByteBuffer {

    private static final String SOURCE_TOPIC = "streams-wordcount-input";
    private static final String TARGET_TOPIC = "sink-conversion-output";

    public static void main(String[] args) {

        /* ---------- consumer config ----------- */
        Properties cProps = new Properties();
        cProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        cProps.put(ConsumerConfig.GROUP_ID_CONFIG, "sink-forwarder-group");
        cProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        cProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        cProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /* ---------- producer config ----------- */
        Properties pProps = new Properties();
        pProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        pProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        pProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(cProps);
             KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(pProps)) {

            SinkRecordConverter converter = new SinkRecordConverter();
            consumer.subscribe(Collections.singletonList(SOURCE_TOPIC));

            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(250));

                for (ConsumerRecord<byte[], byte[]> record : records) {

                    /* ---------- NEW: wrap key/value in ByteBuffer ---------- */
                    ByteBuffer keyBuffer   = record.key()   == null ? null : ByteBuffer.wrap(record.key());
                    ByteBuffer valueBuffer = record.value() == null ? null : ByteBuffer.wrap(record.value());
                    /* ------------------------------------------------------- */

                    SinkRecord sinkRecord = new SinkRecord(
                            record.topic(),
                            record.partition(),
                            Schema.OPTIONAL_BYTES_SCHEMA,
                            keyBuffer,
                            Schema.OPTIONAL_BYTES_SCHEMA,
                            valueBuffer,
                            record.offset(),
                            record.timestamp(),
                            TimestampType.CREATE_TIME,
                            toConnectHeaders(record.headers())   // now returns ByteBuffer values
                    );

                    ProducerRecord<byte[], byte[]> producerRecord =
                            converter.convert(TARGET_TOPIC, sinkRecord);

                    producer.send(producerRecord, (meta, ex) -> {
                        if (ex == null) {
                            System.out.printf("Forwarded to %s-%d@%d%n",
                                    meta.topic(), meta.partition(), meta.offset());
                        } else {
                            ex.printStackTrace();
                        }
                    });
                }
            }
        } catch (WakeupException ignored) {
            // shutting down
        }
    }

    /* -------------------------------------------------------------------- */
    private static Headers toConnectHeaders(
            org.apache.kafka.common.header.Headers kafkaHeaders) {

        Headers connectHeaders = new ConnectHeaders();
        for (Header kHeader : kafkaHeaders) {
            /* ---------- NEW: store as ByteBuffer so the converter hits its branch */
            ByteBuffer bufferVal = kHeader.value() == null ? null : ByteBuffer.wrap(kHeader.value());
            connectHeaders.add(kHeader.key(), bufferVal, Schema.OPTIONAL_BYTES_SCHEMA);
        }
        return connectHeaders;
    }
}
