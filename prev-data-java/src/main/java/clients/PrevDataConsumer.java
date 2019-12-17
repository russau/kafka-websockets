package clients;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

public class PrevDataConsumer {
    public static void main(String[] args) {
        System.out.println("*** Starting Prev Data Consumer ***");
        
        Properties settings = new Properties();
        settings.put(ConsumerConfig.GROUP_ID_CONFIG, "prev-data-consumer");
        settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(settings);

        ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                // nothing to do...
            }
            
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                long startTimestamp = Instant.now().minusSeconds(5 * 60).toEpochMilli();
                
                Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
                for (TopicPartition partition : partitions) {
                    timestampsToSearch.put(partition, startTimestamp);
                }

                Map<TopicPartition, OffsetAndTimestamp> startOffsets = consumer.offsetsForTimes(timestampsToSearch); 
                for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : startOffsets.entrySet()) {
                    if (entry.getValue() != null) {
                        System.out.printf("Seeking partition %d to offset %d\n", entry.getKey().partition(), entry.getValue().offset());
                        consumer.seek(entry.getKey(), entry.getValue().offset());
                    }
                }
            }
        };
        
        try {
            consumer.subscribe(Arrays.asList("driver-positions"), listener);
            int recordCount = 0;
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records)
                {
                    System.out.printf("%s,%s\n", 
                        record.key(), record.value());
                    recordCount++;
                    if (recordCount >= 100) break;
                }
                if (recordCount >= 100) break;
            }
        }
        finally{
            System.out.println("*** Ending Prev Data Consumer ***");
            consumer.close();
        }
    }
}