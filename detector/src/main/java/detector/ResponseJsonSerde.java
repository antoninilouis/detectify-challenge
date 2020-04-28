package detector;

import com.google.gson.Gson;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ResponseJsonSerde extends Serdes.WrapperSerde<String> {

  public ResponseJsonSerde() {
    super(new Serializer<String>() {
      private Gson gson = new Gson();

      @Override
      public void configure(Map<String, ?> map, boolean b) {
      }

      @Override
      public byte[] serialize(String topic, String data) {
        return gson.toJson(data).getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public void close() {
      }
    }, new Deserializer<String>() {
      private Gson gson = new Gson();

      @Override
      public void configure(Map<String, ?> configs, boolean isKey) {

      }

      @Override
      public String deserialize(String topic, byte[] data) {

        return gson.fromJson(new String(data), String.class);
      }

      @Override
      public void close() {

      }
    });
  }
}