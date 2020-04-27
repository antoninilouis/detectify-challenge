package scraper;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import types.HttpResponseDigest;
import types.ScraperReport;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@Slf4j
public class Scraper {

    private static String DETECTION_QUERIES_TOPIC = "detection-queries";
    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String BROKER_HOST = System.getenv("BROKER_HOST");
    private static String BROKER_PORT = System.getenv("BROKER_PORT");
    private static String SCHEMA_REGISTRY_HOST = System.getenv("SCHEMA_REGISTRY_HOST");
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private Consumer<String, String> consumer;
    private Producer<String, ScraperReport> producer;

    public static void main(String[] args) {
        Scraper scraper = new Scraper();

        scraper.start();
    }

    private void start() {
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", BROKER_HOST + ':' + BROKER_PORT);
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("group.id", "scraper");
        consumer = new KafkaConsumer<String,String>(consumerProps);
        consumer.subscribe(Collections.singletonList(DETECTION_QUERIES_TOPIC));

        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", BROKER_HOST + ':' + BROKER_PORT);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        producerProps.put("schema.registry.url", "http://" + SCHEMA_REGISTRY_HOST + ":8081");
        producer = new KafkaProducer<String, ScraperReport>(producerProps);

        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = getConsumerRecords();

                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    Gson gson = new Gson();
                    String[] hostnames = gson.fromJson(consumerRecord.value(), String[].class);

                    for (String hostname : Arrays.asList(hostnames)) {
                        scanHost(consumerRecord.key(), hostname);
                    }
                }
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException ioException) {
                log.error("An exception occured while closing HttpClient: ", ioException);
            }
            consumer.close();
            producer.close();
        }
    }

    /**
     * Use Apache CloseableHttpClient to send GET HTTP requests to provided hosts
     * - Use ResponseHandler to simplify connection management
     * - Use EntityUtils to read all content from the entity
     * 
     * Improvements:
     * - HttpClient could be replaced by an AsyncHttpClient
     * - Performance could be improved using multiple worker threads
     * - Control could be improved by reading content from the Entity using ImputStream
     * - Flexibility could be improved by handling HTTP2
     */
    private HttpResponseDigest sendRequest(String uri) throws IllegalArgumentException {
        HttpResponseDigest httpResponseDigest = null;
        try {
            HttpGet httpget = new HttpGet(uri);
            httpResponseDigest = httpClient.execute(httpget, responseHandler);
        } catch (ClientProtocolException clientProtocolException) {
            log.error("An exception occured while handling HttpResponse: ", clientProtocolException);
        } catch (IOException ioException) {
            log.error("An exception occured during HttpGet execution: ", ioException);
        }
        return httpResponseDigest;
    }

    private ResponseHandler<HttpResponseDigest> responseHandler = new ResponseHandler<HttpResponseDigest>() {

        @Override
        public HttpResponseDigest handleResponse(final HttpResponse response) throws ClientProtocolException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();

                // Handle entity not found
                if (entity == null) {
                    return null;
                }

                return buildHttpResponseDigest(response);
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }
    };

    private HttpResponseDigest buildHttpResponseDigest(HttpResponse response) {
        Header[] requestHeaders = response.getAllHeaders();

        // Build HttpResponseDigest object wrapping header infos
        HttpResponseDigest httpResponseDigest = HttpResponseDigest.newBuilder()

        // Set headers (array)
        .setHeaders(Arrays.asList(requestHeaders).stream().map(requestHeader ->

            // Build Header (record)
            types.Header.newBuilder()
            .setName(requestHeader.getName())
            .setValue(requestHeader.getValue())
            // Set elements (array)
            .setElements(Arrays.asList(requestHeader.getElements()).stream().map(headerElement ->

                // Build Element (record)
                types.Element.newBuilder()
                // Set name (primitive)
                .setName(headerElement.getName())
                // Set value (primitive)
                .setValue(headerElement.getValue())
                .setParameterCount(headerElement.getParameterCount())
                // Set parameters (array)
                .setParameters(Arrays.asList(headerElement.getParameters()).stream().map(elementParam ->

                    // Build parameter (record)
                    types.Parameter.newBuilder()
                    // Set name (primitive)
                    .setName(elementParam.getName())
                    // Set value (primitive)
                    .setValue(elementParam.getValue())
                    .build()
                ).collect(Collectors.toList()))
                .build()
            ).collect(Collectors.toList()))
            .build()
        ).collect(Collectors.toList()))
        .build();
        return httpResponseDigest;
    }

    private ConsumerRecords<String, String> getConsumerRecords() {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10));
        if (!records.isEmpty())
            log.info(records.toString());
        return records;
    }

    private void scanHost(String correlationId, String hostname){
        // Wrap HttpResponseDigest in ScraperReport
        // - includes hostname
        // - includes ip
        // - includes CorrelationID to enable response
    
        HttpResponseDigest httpResponseDigest = null;
        try {
            httpResponseDigest = sendRequest("http://" + hostname);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("An exception occured while setting HttpGet request URI: ", illegalArgumentException);
        }

        ScraperReport scraperReport = ScraperReport.newBuilder()
        .setHostIp("IP")
        .setHostname(hostname)
        .setHttpResponseDigest(httpResponseDigest)
        .setRequesterCorrelationId(correlationId)
        .build();

        ProducerRecord<String, ScraperReport> producerRecord = new ProducerRecord<String, ScraperReport>(SCRAPING_DATA_TOPIC,
            hostname,
            scraperReport
        );
        log.info("Sending scraping-data record.");
        producer.send(producerRecord);
    }
}
