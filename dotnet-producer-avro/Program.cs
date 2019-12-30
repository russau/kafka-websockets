namespace DotnetProducerAvro
{
    using System;
    using System.IO;
    using System.Threading;
    using System.Threading.Tasks;
    using Confluent.Kafka;
    using Confluent.Kafka.SyncOverAsync;
    using Confluent.SchemaRegistry;
    using Confluent.SchemaRegistry.Serdes;
    using solution.model;

    /// <summary>
    /// This class does something.
    /// </summary>
    public class Program
    {
        private const string DriverFilePrefix = "./drivers/";
        private const string KafkaTopic = "driver-positions-avro";

        /// <summary>
        /// This main does something.
        /// </summary>
        /// <param name="args">Not used.</param>
        public static void Main(string[] args)
        {
            var producerConfig = new ProducerConfig { BootstrapServers = "kafka:9092" };
            var schemaRegistryConfig = new SchemaRegistryConfig { Url = "http://schema-registry:8081" };
            string driverId = System.Environment.GetEnvironmentVariable("DRIVER_ID");
            driverId = (!string.IsNullOrEmpty(driverId)) ? driverId : "driver-2";

            Action<DeliveryReport<string, PositionValue>> handler = r =>
                Console.WriteLine(!r.Error.IsError
                    ? $"Delivered message to {r.TopicPartitionOffset}"
                    : $"Delivery Error: {r.Error.Reason}");

            using (var schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryConfig))
            using (var producer = new ProducerBuilder<string, PositionValue>(producerConfig)
                .SetValueSerializer(new AvroSerializer<PositionValue>(schemaRegistry).AsSyncOverAsync())
                .Build())
            {
                var lines = File.ReadAllLines(Path.Combine(DriverFilePrefix, "driver-1" + ".csv"));
                int i = 0;
                while (true)
                {
                    string line = lines[i];
                    double latitude1 = double.Parse(line.Split(",")[0]);
                    double longitude1 = double.Parse(line.Split(",")[1]);
                    var position = new PositionValue { latitude = latitude1, longitude = longitude1 };

                    try
                    {
                        producer.Produce(
                            KafkaTopic,
                            new Message<string, PositionValue> { Key = driverId, Value = position },
                            handler);
                    }
                    catch (ProduceException<string, string> e)
                    {
                        Console.WriteLine($"Delivery failed: {e.Error.Reason}");
                        break;
                    }

                    Thread.Sleep(1000);
                    i = (i + 1) % lines.Length;
                }

                // wait for up to 10 seconds for any inflight messages to be delivered.
                producer.Flush(TimeSpan.FromSeconds(10));
            }
        }
    }

    /*
    Try the command line tools:

    kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic hello-topic-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer

    curl schema-registry:8081/subjects/driver-positions-avro-value/versions/1
    */
}