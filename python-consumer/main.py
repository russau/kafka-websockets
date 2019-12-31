"Python Consumer"
from confluent_kafka import Consumer

KAFKA_TOPIC = "driver-positions"

print("Starting Python Consumer.")

consumer = Consumer({
    'bootstrap.servers': 'kafka:9092',
    'plugin.library.paths': 'monitoring-interceptor',
    'group.id': 'python-consumer',
    'auto.offset.reset': 'earliest'
})

consumer.subscribe([KAFKA_TOPIC])

try:
    while True:
        msg = consumer.poll(1.0)

        if msg is None:
            continue
        if msg.error():
            print("Consumer error: {}".format(msg.error()))
            continue

        print("Key:{} Value:{} [partition {}]".format(
            msg.key().decode('utf-8'),
            msg.value().decode('utf-8'),
            msg.partition()
        ))
except KeyboardInterrupt:
    pass
finally:
    # Leave group and commit final offsets
    print("Closing consumer.")
    consumer.close()
