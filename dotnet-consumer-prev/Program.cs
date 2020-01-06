namespace DotnetConsumer
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Threading;
    using Confluent.Kafka;

    /// <summary>
    /// Dotnet consumer.
    /// </summary>
    public class Program
    {
        private const string KafkaTopic = "driver-positions";

        /// <summary>
        /// Main method for console app.
        /// </summary>
        /// <param name="args">No arguments used.</param>
        public static void Main(string[] args)
        {
            Console.WriteLine("Starting .net consumer.");

            var consumerConfig = new ConsumerConfig
            {
                BootstrapServers = "kafka:9092",
                GroupId = "csharp-consumer-prev",
                AutoOffsetReset = AutoOffsetReset.Earliest,
                PluginLibraryPaths = "monitoring-interceptor",
            };

            using (var consumer = new ConsumerBuilder<string, string>(consumerConfig)
            .SetPartitionsAssignedHandler((c, partitions) =>
            {
                var timestamp = new Confluent.Kafka.Timestamp(DateTime.Now.AddMinutes(-5));
                var timestamps = partitions.Select(tp => new TopicPartitionTimestamp(tp, timestamp));
                var offsets = c.OffsetsForTimes(timestamps, TimeSpan.FromMinutes(1));
                foreach (var offset in offsets)
                {
                    Console.WriteLine($"Moving partion {offset.Partition.Value} to {offset.Offset.Value}");
                }

                return offsets;
            })
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
                    int recordCount = 0;
                    while (true)
                    {
                        try
                        {
                            var cr = consumer.Consume(cts.Token);
                            Console.WriteLine($"{cr.Key},{cr.Value}");
                            recordCount++;
                            if (recordCount >= 100)
                            {
                                break;
                            }
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