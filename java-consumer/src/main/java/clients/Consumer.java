package clients;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Consumer {

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
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
        "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor");

    final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(settings);

    try {
      consumer.subscribe(Arrays.asList("driver-positions"));
      while (true) {
        final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          System.out.printf("%s,%s\n", record.key(), record.value());
        }
      }
    } finally {
      System.out.println("*** Ending Consumer ***");
      consumer.close();
    }
  }
}