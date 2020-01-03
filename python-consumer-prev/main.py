"Python Consumer"
import time
from confluent_kafka import Consumer

KAFKA_TOPIC = "driver-positions"

print("Starting Python Consumer.")

consumer = Consumer({
    'bootstrap.servers': 'kafka:9092',
    'plugin.library.paths': 'monitoring-interceptor',
    'group.id': 'python-consumer-prev',
    'auto.offset.reset': 'earliest'
})

def my_on_assign(konsumer, partitions):
    "On partition assignment move the offsets to 5 min ago"
    timestamp = (time.time() - (5 * 60)) * 1000

    for part in partitions:
        part.offset = timestamp

    new_offsets = konsumer.offsets_for_times(partitions)
    for part in new_offsets:
        print("Setting partition {} to offset {}".format(part.partition, part.offset))
    konsumer.assign(new_offsets)

consumer.subscribe([KAFKA_TOPIC], on_assign=my_on_assign)
record_count = 0

try:
    while True:
        msg = consumer.poll(1.0)

        if msg is None:
            continue
        if msg.error():
            print("Consumer error: {}".format(msg.error()))
            continue

        print("{},{}".format(
            msg.key().decode('utf-8'),
            msg.value().decode('utf-8')
        ))
        record_count += 1
        if record_count >= 100:
            break
except KeyboardInterrupt:
    pass
finally:
    # Leave group and commit final offsets
    print("Closing consumer.")
    consumer.close()
