package clients;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class Producer {
    final static String DRIVER_FILE_PREFIX = "./drivers/";
    final static String KAFKA_TOPIC = "driver-positions";

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("*** Starting VP Producer ***");

        Properties settings = new Properties();
        String bootstrapServer = System.getenv("BOOTSTRAP_SERVERS");
        bootstrapServer = (bootstrapServer != null) ? bootstrapServer : "localhost:29092";
        String driverId  = System.getenv("DRIVER_ID");
        driverId = (driverId != null) ? driverId : "driver-1";

        settings.put(ProducerConfig.CLIENT_ID_CONFIG, driverId);
        settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // settings.put("batch.size", 16384);
        // settings.put("linger.ms", 5000);

        final KafkaProducer<String, String> producer = new KafkaProducer<>(settings);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("### Stopping VP Producer ###");
            producer.close();
        }));

        int pos = 0;
        String[] rows = Files.readAllLines(Paths.get(DRIVER_FILE_PREFIX + driverId + ".csv"), Charset.forName("UTF-8"))
                             .toArray(new String[0]);

        while (true) {
            final String key = driverId;
            final String value = rows[pos];
            final ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, key, value);
            producer.send(record, (md, e) -> {
                System.out.println(String.format("Sent [%s] %s", key, value));
            });
            Thread.sleep(1000);
            pos = (pos + 1) % rows.length;
        }
    }
}