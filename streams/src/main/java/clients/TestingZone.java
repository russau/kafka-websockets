package clients;

import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.geographiclib.Geodesic;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.State;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;

import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

public class TestingZone {

  /**
   * Our first streams app.
   */
  public static void main(String[] args) {

    System.out.println(">>> Starting the vp-streams-app Application");

    // TODO: add code here
    final Properties settings = new Properties();
    settings.put(StreamsConfig.APPLICATION_ID_CONFIG, "vp-streams-app-1");
    settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
    settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
        Serdes.String().getClass().getName());
    settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
        Serdes.String().getClass().getName());
    // Disabling caching ensures we get a complete "changelog" from the
    // aggregate(...) step above (i.e.
    // every input event will have a corresponding output event.
    // see
    // https://kafka.apache.org/23/documentation/streams/developer-guide/memory-mgmt.html#record-caches-in-the-dsl
    settings.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");

    final Topology topology = getTopology();
    System.out.println(topology.describe());
    final KafkaStreams streams = new KafkaStreams(topology, settings);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("<<< Stopping the vp-streams-app Application");
      streams.close();
    }));

    // don't do this in prod as it clears your state stores
    streams.cleanUp();
    streams.start();

    // Scanner scanner = new Scanner(System.in);
    // while (true) {
    //   System.out.println("Streams state: " + streams.state().toString());
    //   System.out.println(">>> Press enter to continue, or 'q' to leave loop: ");
    //   String line = scanner.nextLine();
    //   if (line.startsWith("q")) {
    //     break;
    //   }

    //   if (streams.state() == State.RUNNING) {
    //     ReadOnlyKeyValueStore<String, String> keyValueStore = streams.store(
    //         "queryable-store-name",
    //         QueryableStoreTypes.keyValueStore());
    //     KeyValueIterator<String, String> range = keyValueStore.all();
    //     while (range.hasNext()) {
    //       KeyValue<String, String> next = range.next();
    //       System.out.println("? Value for " + next.key + ": " + next.value);
    //     }
    //   }
    // }
    // scanner.close();
  }

  private static Topology getTopology() {
    final StreamsBuilder builder = new StreamsBuilder();

    // Aggregate ideas from
    // https://github.com/cloudboxlabs/blog-code/blob/master/citibikekafkastreams/src/main/java/com/cloudboxlabs/TurnoverRatio.java#L82
    // https://github.com/confluentinc/kafka-streams-examples/blob/5.3.1-post/src/main/java/io/confluent/examples/streams/interactivequeries/kafkamusic/KafkaMusicExample.java#L362

    final KStream<String, String> testing = builder.stream("driver-positions");
    final KTable<String, String> reduced = testing.groupByKey().aggregate(
        () -> "0,0,0", (key, newValue, accumulator) -> {
        final Double latitude1 = Double.parseDouble(accumulator.split(",")[0]);
        final Double longitude1 = Double.parseDouble(accumulator.split(",")[1]);
        Double lastDistance = Double.parseDouble(accumulator.split(",")[2]);

        final Pattern pattern = Pattern.compile("([-+]?\\d*\\.\\d*),([-+]?\\d*\\.\\d*)");
        final Matcher matcher = pattern.matcher(newValue);
        matcher.find();
        final Double latitude2 = Double.parseDouble(matcher.group(1));
        final Double longitude2 = Double.parseDouble(matcher.group(2));

        if (latitude1 != 0) {
          final Double distance = Geodesic.WGS84.Inverse(latitude1, longitude1,
              latitude2, longitude2).s12;
          lastDistance += distance;
        }
        return latitude2 + "," + longitude2 + "," + lastDistance;
      }, Materialized.as("queryable-store-name"));

    reduced.toStream().to("driver-positions-distance");

    final Topology topology = builder.build();
    return topology;
  }

}