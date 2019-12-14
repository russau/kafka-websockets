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
    System.out.println("*** Starting VP Producer ***");

    final Properties settings = new Properties();
    String bootstrapServer = System.getenv("BOOTSTRAP_SERVERS");
    bootstrapServer = (bootstrapServer != null) ? bootstrapServer : "localhost:29092";
    String driverId  = System.getenv("DRIVER_ID");
    driverId = (driverId != null) ? driverId : "driver-1";

    settings.put(ProducerConfig.CLIENT_ID_CONFIG, driverId);
    settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    settings.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

    final KafkaProducer<String, PositionValue> producer = new KafkaProducer<>(settings);
    
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("### Stopping VP Producer ###");
      producer.close();
    }));

    int pos = 0;
    final String[] rows = Files.readAllLines(Paths.get(DRIVER_FILE_PREFIX + driverId + ".csv"),
      Charset.forName("UTF-8")).toArray(new String[0]);

    while (true) {
      final String key = driverId;
      final Double latitude1 = Double.parseDouble(rows[pos].split(",")[0]);
      final Double longitude1 = Double.parseDouble(rows[pos].split(",")[1]);
      final PositionValue value = new PositionValue();
      value.setLatitude(latitude1);
      value.setLongitude(longitude1);
      final ProducerRecord<String, PositionValue> record = new ProducerRecord<>(
          KAFKA_TOPIC, key, value);
      producer.send(record, (md, e) -> {
        System.out.println(String.format("Sent [%s] %s", key, value));
      });
      Thread.sleep(1000);
      pos = (pos + 1) % rows.length;
    }

    /*
    Try the command line tools:

    kafka-avro-console-consumer --bootstrap-server localhost:29092 \
    --property schema.registry.url=http://localhost:8081 \
    --topic driver-positions-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer

    curl localhost:8081/subjects/driver-positions-avro-value/versions/1
    */

  }
}