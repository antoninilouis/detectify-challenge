package detector;

import lombok.extern.slf4j.Slf4j;
import types.HttpResponseDigest;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

@Slf4j
public class Detector {
    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String DETECTION_DATA_TOPIC = "detection-data";
    private static String BROKER_HOST = System.getenv("BROKER_HOST");
    private static String BROKER_PORT = System.getenv("BROKER_PORT");
    private static String SCHEMA_REGISTRY_HOST = System.getenv("SCHEMA_REGISTRY_HOST");

    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, HttpResponseDigest> stream = builder.stream(SCRAPING_DATA_TOPIC);
        stream.peek((key, value) -> log.info(String.format("HttpResponseDigest for %s: %s", key, value.toString())))
            .to(DETECTION_DATA_TOPIC);
        Topology topology = builder.build();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "detector");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER_HOST + ":" + BROKER_PORT);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://" + SCHEMA_REGISTRY_HOST + ":8081");

        KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
