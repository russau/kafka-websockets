"Python Consumer"
from confluent_kafka import Consumer

KAFKA_TOPIC = "driver-positions"

print("Starting Consumer (Python)")

consumer = Consumer({
    'bootstrap.servers': 'kafka:9092',
    'plugin.library.paths': 'monitoring-interceptor',
    'group.id': 'python-consumer',
    'auto.offset.reset': 'earliest'
})

consumer.subscribe([KAFKA_TOPIC])

while True:
    msg = consumer.poll(1.0)

    if msg is None:
        continue
    if msg.error():
        print("Consumer error: {}".format(msg.error()))
        continue

    print('Received message: {}'.format(msg.value().decode('utf-8')))
