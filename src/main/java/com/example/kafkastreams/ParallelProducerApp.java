package com.example.kafkastreams;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ParallelProducerApp {

    public static void main(String[] args) {
        Thread userThread = new Thread(new UserProducer());
        Thread orderThread = new Thread(new OrderProducer());

        userThread.start();
        orderThread.start();

        try {
            userThread.join();
            orderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class UserProducer implements Runnable {
        @Override
        public void run() {
            Producer<String, String> producer = new KafkaProducer<>(getProducerProperties("user-producer-client"));

            for (int i = 1; i <= 500; i++) {
                producer.send(new ProducerRecord<>("parallel-topic1", "user-" + i, "UserName" + i));
                System.out.println("Produced message to parallel-topic1");
                sleep(5000); // Simulate delay
            }

            producer.close();
            System.out.println("✅ UserProducer finished.");
        }
    }

    static class OrderProducer implements Runnable {
        @Override
        public void run() {
            Producer<String, String> producer = new KafkaProducer<>(getProducerProperties("order-producer-client"));

            for (int i = 1; i <= 500; i++) {
                producer.send(new ProducerRecord<>("parallel-topic2", "order-" + i, "OrderAmount" + (i * 100)));
                System.out.println("Produced message to parallel-topic2");
                sleep(5000); // Simulate delay
            }

            producer.close();
            System.out.println("✅ OrderProducer finished.");
        }
    }

    private static Properties getProducerProperties(String clientId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9093");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("client.id", clientId); // 👈 Unique client ID for metrics
        return props;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
