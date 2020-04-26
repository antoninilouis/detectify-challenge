package scraper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import types.HttpResponseDigest;
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

    private static String SCRAPING_DATA_TOPIC = "scraping-data";
    private static String BROKER_HOST = System.getenv("BROKER_HOST");
    private static String BROKER_PORT = System.getenv("BROKER_PORT");
    private static String SCHEMA_REGISTRY_HOST = System.getenv("SCHEMA_REGISTRY_HOST");
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();

    public static void main(String[] args) {
        Scraper scraper = new Scraper();

        Properties props = new Properties();
        props.put("bootstrap.servers", BROKER_HOST + ':' + BROKER_PORT);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", "http://" + SCHEMA_REGISTRY_HOST + ":8081");

        String hostname = "nginx";
        HttpResponseDigest httpResponseDigest = null;
        try {
            httpResponseDigest = scraper.sendRequest("http://" + hostname);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("An exception occured while setting HttpGet request URI: ", illegalArgumentException);
        }

        Producer<String, HttpResponseDigest> producer = new KafkaProducer<String, HttpResponseDigest>(props);
        ProducerRecord<String, HttpResponseDigest> producerRecord = new ProducerRecord<String, HttpResponseDigest>(SCRAPING_DATA_TOPIC,
            hostname,
            httpResponseDigest
        );
        log.info("Sending scraping-data record.");
        producer.send(producerRecord);
        producer.close();
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
        } finally {
            try {
                httpClient.close();
            } catch (IOException ioException) {
                log.error("An exception occured while closing HttpClient: ", ioException);
            }
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
}
