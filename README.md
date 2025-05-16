# Kafka Streams WordCount Example

This project demonstrates a basic Kafka Streams application that performs a word count on text input and writes the results to an output topic.

## Prerequisites

- Docker and Docker Compose installed
- Java 11+ and Maven (to build and run `WordCountApp.java`)

## WordCountApp

### 1. Start Kafka and Zookeeper

Start the services using Docker Compose:

```bash
docker-compose up -d
```
- This starts:
    - Zookeeper on port 2181
    - Kafka on port 9093


### 2. List all topics to verify

```shell
docker exec -it kafka-streams-kaspar-kafka-1 \
  kafka-topics --list \
    --bootstrap-server localhost:9093
```
### 3. Produce Messages to Input Topic
In one terminal, start a producer and type a few sentences (press Enter after each):

```shell
docker exec -it kafka-streams-kaspar-kafka-1 \
  kafka-console-producer \
    --broker-list localhost:9093 \
    --topic streams-wordcount-input
```

Example input:

```text
Hello Kafka Streams
Kafka Streams is cool
Streams process data fast
```

### 3. Start the WordCountApp application

Use the `WordCountApp.xml` run configuration for IntelliJ IDEA.
Or run java from command line:
```text
mvn clean package
java -cp target/your-jar-file.jar com.example.kafkastreams.WordCountApp
```

### 4. Consume from Output Topic

```shell
docker exec -it kafka-streams-kaspar-kafka-1 \
  kafka-console-consumer \
    --bootstrap-server localhost:9093 \
    --topic streams-wordcount-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

You should see output similar to:
```text
hello   1
kafka   2
streams 3
is      1
cool    1
process 1
data    1
fast    1
```

## WordCountWindowedApp
```shell
docker exec -it kafka-streams-kaspar-kafka-1 \                                                                                                                                               7s
  kafka-console-producer \
    --broker-list localhost:9093 \
    --topic streams-windowed-wordcount-input
```

```shell
docker exec -it kafka-streams-kaspar-kafka-1 \
  kafka-console-consumer \
    --bootstrap-server localhost:9093 \
    --topic streams-windowed-wordcount-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```