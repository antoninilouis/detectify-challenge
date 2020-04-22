package scraper;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Scraper {

    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String brokerHost = System.getenv("BROKER_HOST");
    private static String brokerPort = System.getenv("BROKER_PORT");

    public static void main(String[] args) {
        Properties props = new Properties();

        props.put("bootstrap.servers", brokerHost + ':' + brokerPort);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String,String>(SCRAPING_DATA_TOPIC, "key", "value");
        log.info("Sending scraping-data record.");
        producer.send(producerRecord);
        producer.close();
    }
}
