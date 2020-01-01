namespace DotnetConsumerAvro
{
    using System;
    using System.Threading;
    using clients.avro;
    using Confluent.Kafka;
    using Confluent.Kafka.SyncOverAsync;
    using Confluent.SchemaRegistry;
    using Confluent.SchemaRegistry.Serdes;

    /// <summary>
    /// Dotnet avro consumer.
    /// </summary>
    public class Program
    {
        private const string KafkaTopic = "driver-positions-avro";

        /// <summary>
        /// Main method for console app.
        /// </summary>
        /// <param name="args">No arguments used.</param>
        public static void Main(string[] args)
        {
            Console.WriteLine("Starting .net Avro consumer.");

            var consumerConfig = new ConsumerConfig
            {
                BootstrapServers = "kafka:9092",
                GroupId = "csharp-consumer-avro",
                AutoOffsetReset = AutoOffsetReset.Earliest,
                PluginLibraryPaths = "monitoring-interceptor",
            };
            var schemaRegistryConfig = new SchemaRegistryConfig { Url = "http://schema-registry:8081" };

            using (var schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryConfig))
            using (var consumer = new ConsumerBuilder<string, PositionValue>(consumerConfig)
                .SetValueDeserializer(new AvroDeserializer<PositionValue>(schemaRegistry).AsSyncOverAsync())
                .Build())
            {
                consumer.Subscribe(KafkaTopic);

                CancellationTokenSource cts = new CancellationTokenSource();
                Console.CancelKeyPress += (_, e) =>
                {
                    e.Cancel = true; // prevent the process from terminating.
                    cts.Cancel();
                };

                try
                {
                    while (true)
                    {
                        try
                        {
                            var cr = consumer.Consume(cts.Token);
                            Console.WriteLine($"Key:{cr.Key} Latitude:{cr.Value.latitude} Longitude:{cr.Value.longitude} [partition {cr.Partition.Value}]");
                        }
                        catch (ConsumeException e)
                        {
                            Console.WriteLine($"Error occured: {e.Error.Reason}");
                        }
                    }
                }
                catch (OperationCanceledException)
                {
                    // Ensure the consumer leaves the group cleanly and final offsets are committed.
                    Console.WriteLine("Closing consumer.");
                    consumer.Close();
                }
            }
        }
    }
}