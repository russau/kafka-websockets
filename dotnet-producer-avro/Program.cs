namespace DotnetProducerAvro
{
    using System;
    using System.IO;
    using System.Threading;
    using System.Threading.Tasks;
    using Confluent.Kafka;
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
        /// <returns>nothing.</returns>
        public static async Task Main(string[] args)
        {
            var producerConfig = new ProducerConfig { BootstrapServers = "kafka:9092" };
            var schemaRegistryConfig = new SchemaRegistryConfig { Url = "http://schema-registry:8081" };

            using (var schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryConfig))
            using (var producer = new ProducerBuilder<string, PositionValue>(producerConfig)
                .SetValueSerializer(new AvroSerializer<PositionValue>(schemaRegistry))
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

                    await producer
                        .ProduceAsync(KafkaTopic, new Message<string, PositionValue> { Key = "dotnet-1", Value = position })
                        .ContinueWith(task =>
                        {
                            Console.WriteLine(task.IsFaulted
                            ? $"error producing message: {task.Exception.Message}"
                            : $"produced to: {task.Result.TopicPartitionOffset}");
                        });
                    Thread.Sleep(1000);
                    i = (i + 1) % lines.Length;
                }
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