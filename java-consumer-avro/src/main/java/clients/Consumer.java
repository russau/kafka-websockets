package clients;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import solution.model.PositionValue;

public class Consumer {
  static final String KAFKA_TOPIC = "driver-positions-avro";

  /**
   * Java consumer.
   */
  public static void main(String[] args) {
    System.out.println("*** Starting Consumer ***");

    final Properties settings = new Properties();
    settings.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer");
    settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
    settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
    settings.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
    settings.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://schema-registry:8081");

    final KafkaConsumer<String, PositionValue> consumer = new KafkaConsumer<>(settings);

    try {
      consumer.subscribe(Arrays.asList(KAFKA_TOPIC));
      while (true) {
        final ConsumerRecords<String, PositionValue> records =
            consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, PositionValue> record : records) {
          System.out.printf("%s,%s,%s\n",
              record.key(),
              record.value().getLatitude(),
              record.value().getLongitude());
        }
      }
    } finally {
      System.out.println("*** Ending Consumer ***");
      consumer.close();
    }
  }
}