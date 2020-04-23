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

        Producer<String, String> producer = new KafkaProducer<String, String>(props);
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String,String>(SCRAPING_DATA_TOPIC, 
            "{\r\n  \"schema\": {\r\n    \"type\": \"struct\",\r\n    \"fields\": [\r\n      {\r\n        \"type\": \"int64\",\r\n        \"optional\": false,\r\n        \"field\": \"registertime\"\r\n      },\r\n      {\r\n        \"type\": \"string\",\r\n        \"optional\": false,\r\n        \"field\": \"userid\"\r\n      },\r\n      {\r\n        \"type\": \"string\",\r\n        \"optional\": false,\r\n        \"field\": \"regionid\"\r\n      },\r\n      {\r\n        \"type\": \"string\",\r\n        \"optional\": false,\r\n        \"field\": \"gender\"\r\n      }\r\n    ],\r\n    \"optional\": false,\r\n    \"name\": \"ksql.users\"\r\n  },\r\n  \"payload\": {\r\n    \"registertime\": 1493819497170,\r\n    \"userid\": \"User_1\",\r\n    \"regionid\": \"Region_5\",\r\n    \"gender\": \"MALE\"\r\n  }\r\n}",
            "{\r\n  \"schema\": {\r\n    \"type\": \"struct\",\r\n    \"fields\": [\r\n      {\r\n        \"type\": \"int64\",\r\n        \"optional\": false,\r\n        \"field\": \"registertime\"\r\n      },\r\n      {\r\n        \"type\": \"string\",\r\n        \"optional\": false,\r\n        \"field\": \"userid\"\r\n      },\r\n      {\r\n        \"type\": \"string\",\r\n        \"optional\": false,\r\n        \"field\": \"regionid\"\r\n      },\r\n      {\r\n        \"type\": \"string\",\r\n        \"optional\": false,\r\n        \"field\": \"gender\"\r\n      }\r\n    ],\r\n    \"optional\": false,\r\n    \"name\": \"ksql.users\"\r\n  },\r\n  \"payload\": {\r\n    \"registertime\": 1493819497170,\r\n    \"userid\": \"User_1\",\r\n    \"regionid\": \"Region_5\",\r\n    \"gender\": \"MALE\"\r\n  }\r\n}"
        );
        log.info("Sending scraping-data record.");
        producer.send(producerRecord);
        producer.close();
    }
}
