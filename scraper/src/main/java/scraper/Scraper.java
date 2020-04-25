package scraper;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import types.User;

@Slf4j
public class Scraper {

    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String BROKER_HOST = System.getenv("BROKER_HOST");
    private static String BROKER_PORT = System.getenv("BROKER_PORT");
    private static String SCHEMA_REGISTRY_HOST = System.getenv("SCHEMA_REGISTRY_HOST");

    public static void main(String[] args) {
        Properties props = new Properties();

        props.put("bootstrap.servers", BROKER_HOST + ':' + BROKER_PORT);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", "http://" + SCHEMA_REGISTRY_HOST + ":8081");

        Producer<String, User> producer = new KafkaProducer<String, User>(props);
        ProducerRecord<String, User> producerRecord = new ProducerRecord<String, User>(SCRAPING_DATA_TOPIC, 
            "Record ID",
            new User("John", "Doe", 33)
        );
        log.info("Sending scraping-data record.");
        producer.send(producerRecord);
        producer.close();
    }
}
