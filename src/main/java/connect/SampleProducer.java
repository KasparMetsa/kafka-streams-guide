package connect;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SampleProducer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < 10; i++) {
                byte[] key   = ("key-"   + i).getBytes(StandardCharsets.UTF_8);
                byte[] value = ("value-" + i).getBytes(StandardCharsets.UTF_8);

                ProducerRecord<byte[], byte[]> record =
                        new ProducerRecord<>("streams-wordcount-input", key, value);

                /* ---------- NEW: attach a couple of demo headers --------- */
                record.headers()
                        .add(new RecordHeader("producer-id",  //
                                "sample-producer".getBytes(StandardCharsets.UTF_8)))
                        .add(new RecordHeader("sequence",     //
                                ("seq-" + i).getBytes(StandardCharsets.UTF_8)));
                /* --------------------------------------------------------- */

                producer.send(record, (meta, ex) -> {
                    if (ex == null) {
                        System.out.printf("Produced to %s-%d@%d%n",
                                meta.topic(), meta.partition(), meta.offset());
                    } else {
                        ex.printStackTrace();
                    }
                });
            }
            producer.flush();
        }
    }
}
