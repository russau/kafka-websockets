namespace DotnetProducer
{
    using System;
    using System.IO;
    using System.Threading;
    using Confluent.Kafka;

    /// <summary>
    /// Dotnet producer.
    /// </summary>
    public class Program
    {
        private const string DriverFilePrefix = "./drivers/";
        private const string KafkaTopic = "driver-positions";

        /// <summary>
        /// Main method for console app.
        /// </summary>
        /// <param name="args">No arguments used.</param>
        public static void Main(string[] args)
        {
            Console.WriteLine("Starting .net producer.");
            var producerConfig = new ProducerConfig { BootstrapServers = "kafka:9092", PluginLibraryPaths = "monitoring-interceptor" };
            string driverId = System.Environment.GetEnvironmentVariable("DRIVER_ID");
            driverId = (!string.IsNullOrEmpty(driverId)) ? driverId : "driver-2";

            Action<DeliveryReport<string, string>> handler = r =>
                Console.WriteLine(!r.Error.IsError
                    ? $"Sent Key:{r.Message.Key} Value:{r.Message.Value}"
                    : $"Delivery Error: {r.Error.Reason}");

            using (var producer = new ProducerBuilder<string, string>(producerConfig).Build())
            {
                Console.CancelKeyPress += (sender, e) =>
                {
                    // wait for up to 10 seconds for any inflight messages to be delivered.
                    Console.WriteLine("Flushing producer and exiting.");
                    producer.Flush(TimeSpan.FromSeconds(10));
                };

                var lines = File.ReadAllLines(Path.Combine(DriverFilePrefix, driverId + ".csv"));
                int i = 0;
                while (true)
                {
                    string line = lines[i];
                    try
                    {
                        producer.Produce(KafkaTopic, new Message<string, string> { Key = driverId, Value = line }, handler);
                    }
                    catch (ProduceException<string, string> e)
                    {
                        Console.WriteLine($"Delivery failed: {e.Error.Reason}");
                        break;
                    }

                    Thread.Sleep(1000);
                    i = (i + 1) % lines.Length;
                }
            }
        }
    }
}