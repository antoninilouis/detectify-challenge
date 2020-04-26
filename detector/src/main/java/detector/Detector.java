package detector;

import lombok.extern.slf4j.Slf4j;
import types.HttpResponseDigest;
import types.HttpServer;
import types.ServerScan;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.ValueMapper;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

/**
 * A Kafka streaming application to identify the technology used by a server.
 * The Detector uses scraping-data topic HttpResponseDigest serialized data as source
 */
@Slf4j
public class Detector {
    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String HTTP_SERVER_DATA = "http-server-data";
    private static String SERVER_SCAN_DATA = "server-scan-data";
    private static String BROKER_HOST = System.getenv("BROKER_HOST");
    private static String BROKER_PORT = System.getenv("BROKER_PORT");
    private static String SCHEMA_REGISTRY_HOST = System.getenv("SCHEMA_REGISTRY_HOST");

    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();

        // Build stream
        KStream<String, HttpResponseDigest> stream = builder.stream(SCRAPING_DATA_TOPIC);
        // Build http server topology
        stream
            .flatMap(new KeyValueMapper<String, HttpResponseDigest, Iterable<KeyValue<String, HttpServer>>> (){
                @Override
                public Iterable<KeyValue<String, HttpServer>> apply(String key, HttpResponseDigest value) {
                    return Arrays.asList(KeyValue.pair(key,
                        HttpServer.newBuilder()
                        .setHostname(key)
                        .setIp("IP")
                        .build()
                    ));
                }
            })
            .to(HTTP_SERVER_DATA);

        // Build server scan topology
        stream
            .flatMap(new KeyValueMapper<String, HttpResponseDigest, Iterable<KeyValue<String, ServerScan>>> (){
                @Override
                public Iterable<KeyValue<String, ServerScan>> apply(String key, HttpResponseDigest value) {
                    return Arrays.asList(KeyValue.pair(key,
                        ServerScan.newBuilder()
                        .setHttpServerHostname(key)
                        .setTechnology("TECHNOLOGY")
                        .build()
                    ));
                }
            })
            .to(SERVER_SCAN_DATA);

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
