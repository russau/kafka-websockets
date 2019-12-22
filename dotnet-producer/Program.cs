namespace DotnetProducer
{
    using System;
    using Confluent.Kafka;

    /// <summary>
    /// This class does something.
    /// </summary>
    public class Program
    {
        /// <summary>
        /// This main does something.
        /// </summary>
        /// <param name="args">Not used.</param>
        public static void Main(string[] args)
        {
            var conf = new ProducerConfig { BootstrapServers = "kafka:9092" };

            Action<DeliveryReport<Null, string>> handler = r =>
                Console.WriteLine(!r.Error.IsError
                    ? $"Delivered message to {r.TopicPartitionOffset}"
                    : $"Delivery Error: {r.Error.Reason}");

            using (var p = new ProducerBuilder<Null, string>(conf).Build())
            {
                for (int i = 0; i < 100; ++i)
                {
                    p.Produce("my-topic", new Message<Null, string> { Value = i.ToString() }, handler);
                }

                // wait for up to 10 seconds for any inflight messages to be delivered.
                p.Flush(TimeSpan.FromSeconds(10));
            }
        }
    }
}