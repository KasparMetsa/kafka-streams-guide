package connect;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A helper class for converting Kafka Connect Sink Records to Kafka Producer Records to be sent to a
 * remote Kafka cluster.
 */
public class SinkRecordConverter {

    /**
     * Convert the Connect Sink Record into a Kafka Producer Record without altering the content, except
     * for the target topic
     *
     * @param targetTopic the topic name in the remote cluster
     * @param sinkRecord the sink record to convert
     * @return the producer record for the provided sink record
     */
    public ProducerRecord<byte[], byte[]> convert(final String targetTopic, final SinkRecord sinkRecord) {
        requireNonNull(targetTopic);
        requireNonNull(sinkRecord);

        byte[] keyData = convertData(sinkRecord.key());
        byte[] valueData = convertData(sinkRecord.value());
        RecordHeaders headers = convertHeaders(sinkRecord.headers());
        return new ProducerRecord<>(targetTopic, sinkRecord.kafkaPartition(), sinkRecord.timestamp(), keyData, valueData, headers);
    }

    private RecordHeaders convertHeaders(Headers headers) {
        List<RecordHeader> newHeaders = new ArrayList<>(headers.size());
        for (Header header : headers) {
            newHeaders.add(new RecordHeader(header.key(), convertData(header.value())));
        }
        return new RecordHeaders(newHeaders.toArray(new RecordHeader[0]));
    }

    private byte[] convertData(Object data) {
        if (data == null) {
            // IMPORTANT: Return null instead of empty byte array to preserve null semantics
            return null;
        }
        if (data instanceof byte[] bytes) {
            return bytes;
        }
        if (data instanceof ByteBuffer buffer) {
            // For ByteBuffer, we need to handle position and limit properly
            byte[] bytes = new byte[buffer.remaining()];
            // Save current position
            int position = buffer.position();
            buffer.get(bytes);
            // Reset position to original to avoid side effects
            buffer.position(position);
            return bytes;
        }

        throw new IllegalArgumentException("Unknown data type, " + data.getClass().getName());
    }
}