"Python Producer"
from time import sleep
import os
import atexit

from confluent_kafka import Producer

DRIVER_FILE_PREFIX = "./drivers/"
KAFKA_TOPIC = "driver-positions"
DRIVER_ID = os.getenv("DRIVER_ID", "driver-3")

print("Starting Producer (Python)")

producer = Producer({
    'bootstrap.servers': 'kafka:9092',
    'partitioner': 'murmur2_random'
})

def delivery_report(err, msg):
    """ Called once for each message produced to indicate delivery result.
        Triggered by poll() or flush(). """
    if err is not None:
        print('Message delivery failed: {}'.format(err))
    else:
        print(' {} [partition {}]'.format(msg.value().decode(), msg.partition()))


def exit_handler():
    """Run this on exit"""
    print("Stopping Producer (Python)")
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
    producer.produce(
        KAFKA_TOPIC,
        value=line.encode('utf-8').strip(),
        key=DRIVER_ID.encode('utf-8'),
        callback=delivery_report)
    sleep(1)
    pos = (pos + 1) % len(lines)
