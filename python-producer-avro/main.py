"Python Producer"
from time import sleep
import os
import atexit

from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer

DRIVER_FILE_PREFIX = "./drivers/"
KAFKA_TOPIC = "driver-positions-pyavro"
DRIVER_ID = os.getenv("DRIVER_ID", "driver-3")

print("Starting Python Avro producer.")

value_schema = avro.load("position_value.avsc")
key_schema = avro.load("position_key.avsc")

producer = AvroProducer({
    'bootstrap.servers': 'kafka:9092',
    'plugin.library.paths': 'monitoring-interceptor',
    'partitioner': 'murmur2_random',
    'schema.registry.url': 'http://schema-registry:8081'
}
                        , default_key_schema=key_schema
                        , default_value_schema=value_schema)

def exit_handler():
    """Run this on exit"""
    print("Flushing producer and exiting.")
    # Wait for any outstanding messages to be delivered and delivery report
    # callbacks to be triggered.
    producer.flush()

atexit.register(exit_handler)

with open(os.path.join(DRIVER_FILE_PREFIX, DRIVER_ID + ".csv")) as f:
    lines = f.readlines()

pos = 0
while True:
    line = lines[pos]
    # Trigger any available delivery report callbacks from previous produce() calls
    producer.poll(0)
    key = {"key" : DRIVER_ID}
    latitude = line.split(",")[0].strip()
    longitude = line.split(",")[1].strip()
    value = {"latitude" : float(latitude), "longitude" : float(longitude)}
    producer.produce(
        topic=KAFKA_TOPIC,
        value=value,
        key=key,
        callback=lambda err, msg:
        print("Sent Key:{} Value:{}".format(key, value) if err is None else err)
        )
    sleep(1)
    pos = (pos + 1) % len(lines)

# Confirm the topic is being written to with kafka-avro-console-consumer
#
# kafka-avro-console-consumer --bootstrap-server kafka:9092 \
#     --property schema.registry.url=http://schema-registry:8081 \
#     --topic driver-positions-pyavro --property print.key=true \
# --from-beginning
