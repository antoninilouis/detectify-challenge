package detector;

import lombok.extern.slf4j.Slf4j;
import types.Header;
import types.HttpResponseDigest;
import types.HttpServer;
import types.ScraperReport;
import types.ServerScan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import com.google.gson.JsonArray;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Produced;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

/**
 * A Kafka streaming application to identify the technology used by a server.
 * The Detector uses scraping-data topic ScraperReport serialized data as source
 */
@Slf4j
public class Detector {
    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String DETECTION_RESPONSES_TOPIC = "detection-responses";
    private static String HTTP_SERVER_DATA_TOPIC = "http-server-data";
    private static String SERVER_SCAN_DATA_TOPIC = "server-scan-data";
    private static String BROKER_HOST = System.getenv("BROKER_HOST");
    private static String BROKER_PORT = System.getenv("BROKER_PORT");
    private static String SCHEMA_REGISTRY_HOST = System.getenv("SCHEMA_REGISTRY_HOST");

    // Technologies
    private static final String TECHNOLOGY_NGINX = "Nginx";

    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();

        // Build stream
        KStream<String, ScraperReport>stream = builder.stream(SCRAPING_DATA_TOPIC);
        // Build http server topology
        stream
            .flatMap(new KeyValueMapper<String, ScraperReport, Iterable<KeyValue<String, HttpServer>>> (){
                @Override
                public Iterable<KeyValue<String, HttpServer>> apply(String key, ScraperReport scraperReport) {
                    return Arrays.asList(KeyValue.pair(key,
                        HttpServer.newBuilder()
                        .setHostname(scraperReport.getHostname())
                        .setIp(scraperReport.getHostIp())
                        .build()
                    ));
                }
            })
            .to(HTTP_SERVER_DATA_TOPIC);

        // Build server scan topology
        stream
            .flatMap(new KeyValueMapper<String, ScraperReport, Iterable<KeyValue<String, ServerScan>>> (){
                @Override
                public Iterable<KeyValue<String, ServerScan>> apply(String hostname, ScraperReport scraperReport) {
                    List<String> technologies = detectTechnologies(scraperReport.getHttpResponseDigest());
                    return technologies.stream().map(technology -> {
                        KeyValue<String, ServerScan> kv = KeyValue.pair(hostname,
                            ServerScan.newBuilder()
                            .setHttpServerHostname(hostname)
                            .setTechnology(technology)
                            .build()
                        );
                        log.info("KeyValue(): " + kv.toString());
                        return kv;
                    }).collect(Collectors.toList());
                }
            })
            .to(SERVER_SCAN_DATA_TOPIC);

        // Build string topology
        // Produce a record to detection-responses topic with requester correlation id as key and matchList as value
        // Build stream
        stream
            .flatMap(new KeyValueMapper<String, ScraperReport, Iterable<KeyValue<String, Object>>> (){
                @Override
                public Iterable<KeyValue<String, Object>> apply(String key, ScraperReport scraperReport) {
                    List<String> technologies = detectTechnologies(scraperReport.getHttpResponseDigest());
                    JsonArray matchList = new JsonArray();
                    technologies.forEach(technology -> matchList.add(technology));
                    return Collections.singletonList(KeyValue.pair(scraperReport.getRequesterCorrelationId().toString(), matchList.toString()));
                }
            })
            .to(DETECTION_RESPONSES_TOPIC, Produced.with(Serdes.String(), Serdes.serdeFrom(new KafkaJsonSerializer<Object>(), new KafkaJsonDeserializer<Object>())));

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

    private static List<String> detectTechnologies(HttpResponseDigest httpResponseDigest) {
        List<String> technologies = new ArrayList<String>();
        // Detect Nginx server
        Header serverHeader = getHeaderByName(httpResponseDigest.getHeaders(), "Server");
        if (serverHeader.getValue().toString().toLowerCase().contains("nginx"))
            technologies.add(TECHNOLOGY_NGINX);
        log.info("detectTechnologies: " + technologies.toString());
        return technologies;
    }

    private static Header getHeaderByName(List<Object> headers, String name) {
        List<Object> matchList = headers
            .stream()
            .filter(header -> ((Header)header).getName().toString().equals(name))
            .collect(Collectors.toList());
        log.info("getHeaderByName: " + matchList.toString());
        return matchList.size() > 0 ? (Header)matchList.get(0) : null;
    }
}
