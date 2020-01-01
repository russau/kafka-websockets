package clients;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import solution.model.PositionValue;

public class Producer {
  static final String DRIVER_FILE_PREFIX = "./drivers/";
  static final String KAFKA_TOPIC = "driver-positions-avro";

  /**
   * Java producer.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Starting Java Avro producer.");

    final Properties settings = new Properties();
    String driverId  = System.getenv("DRIVER_ID");
    driverId = (driverId != null) ? driverId : "driver-1";

    settings.put(ProducerConfig.CLIENT_ID_CONFIG, driverId);
    settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    settings.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://schema-registry:8081");

    final KafkaProducer<String, PositionValue> producer = new KafkaProducer<>(settings);
    
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Closing producer.");
      producer.close();
    }));

    int pos = 0;
    final String[] rows = Files.readAllLines(Paths.get(DRIVER_FILE_PREFIX + driverId + ".csv"),
      Charset.forName("UTF-8")).toArray(new String[0]);

    while (true) {
      final String key = driverId;
      final Double latitude1 = Double.parseDouble(rows[pos].split(",")[0]);
      final Double longitude1 = Double.parseDouble(rows[pos].split(",")[1]);
      final PositionValue value = new PositionValue(latitude1, longitude1);
      final ProducerRecord<String, PositionValue> record = new ProducerRecord<>(
          KAFKA_TOPIC, key, value);
      producer.send(record, (md, e) -> {
        System.out.println(String.format("Sent Key:%s Latitude:%s Longitude:%s",
            key, value.getLatitude(), value.getLongitude()));
      });
      Thread.sleep(1000);
      pos = (pos + 1) % rows.length;
    }

    /*
    Try the command line tools:

    kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-positions --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer

    curl schema-registry:8081/subjects/driver-positions-avro-value/versions/1
    */

  }
}