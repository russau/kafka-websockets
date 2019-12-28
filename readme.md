### TODO

* lots more commenting
* align all the producer / consumer output, e.g. `"*** Starting VP Producer ***"`
* remove all references to `VP`
* Better package names for avro generated stuff, rename `TestingZone`
* consistent consumer group ids - e.g `java-consumer` ?


### Hosts file

If you are running this on your laptop add these entries to your `/etc/hosts` file.

```
127.0.0.1 kafka
127.0.0.1 schema-registry
127.0.0.1 connect
127.0.0.1 postgres
127.0.0.1 ksql-server
```

### Exercise Ideas

#### Console getting started

```
kafka-topics
  --bootstrap-server
  --create
  --list
  --topic
  --replication-factor
  --partitions
  
kafka-console-producer
  --broker-list
  --topic
  
kafka-console-consumer
  --bootstrap-server
  --topic
  --from-beginning
```

* The kafka cluster in the docker containers has a single broker - what is the maximum replication factor you can configure for a topic in this cluster?
* Experiment with `kafka-console-producer --property parse.key=true --property key.separator=,` to write messages with a key and value.
* Experiment with `kafka-console-consumer --property print.key=true` to view the messages you just created with keys.
* `kafka-console-consumer` takes a parameter `--partition`, view all the messages on partition 1. What is the relationship between keys and partitions?
  
#### Kafka Producer (Java, .net, Python)

`docker-compose up -d kafka zookeeper control-center populate webserver1`

Configure the `bootstrap-server` property. Implement the two (or so) lines of code that do the send. Challenge: can you update the code to print the offset of each message?

```
final ProducerRecord<String, String> record = ...
producer.send(...
```

View the messages with the `kafka-console-consumer`, control-center, and webserver.  Experiment with different settings for `batch.size` and `linger.ms`. 

#### Kafka Consumer (Java, .net, Python)

`docker-compose up -d kafka zookeeper control-center populate webserver1`

`gradle run` the Java producer. Implement the `poll` and printing code.

```
final ConsumerRecords<String, String> records = consumer....
for (ConsumerRecord<String, String> record : records) {
  System.out.printf(...
}
```

Experiment with different settings for `fetch.min.bytes` and `fetch.max.wait.ms` https://kafka.apache.org/documentation/#consumerconfigs.

* End your processing, and launch the consumer again. You'll see that the second time you run the application processing begins from a non-zero offset. Does `auto.offset.reset` apply the second time the application is run? How do consumers know where to begin their processing? Can you think of a way to make your next run of the application begin at offset zero? (change the group id, or ConsumerRebalanceListener and seek).
* Experiment with the parameters of the `kafka-consumer-groups` command-line utility to find information about your consumer group. Can you find the *lag*  property of your consumer group?

#### Kafka Consumer - offsetsForTimes (Java, .net, Python)

`docker-compose up -d kafka zookeeper control-center populate webserver1`

If there isn't already data in the `driver-positions` topic run the producer from a previous exercise for 5 minutes or so. Implement just the `timestampsToSearch.put(...` and `consumer.seek(...` lines.  Paste the results into the map to see where the car was 5 minutes ago.  Challenge: can you modify the code to seek the beginning offset (beginningOffsets)?

#### Schema Registry and Avro Producer / Consumer (Java, .net, Python)

```
docker-compose up -d kafka zookeeper control-center populate 
docker-compose up -d schema-registry webserver2
```

Generate the specific Avro class for `position_value.avsc`. Java: `gradle build`, .net: `dotnet tool install -g Confluent.Apache.Avro.AvroGen` `avrogen -s position_value.avsc`.  Implement the missing code:

```
final PositionValue value = new PositionValue(...
final ProducerRecord<String, PositionValue> record = new ProducerRecord<>(...
```

See the output on the console...  Now see the java / .net / python edition of avro consumer.

```
kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-positions-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```

* Which Serializers are you using for the key and value of your Kafka messages?
* `curl schema-registry:8081/subjects` `curl schema-registry:8081/subjects/driver-positions-avro-value/versions/` `curl schema-registry:8081/subjects/driver-positions-avro-value/versions/1`
* `curl schema-registry:8081/subjects/driver-positions-avro-value/versions/1 | jq ".schema | fromjson"`
* `curl schema-registry:8081/config`

Coming soon! https://github.com/confluentinc/schema-registry/commit/5bededd7f1f62c4fb3b56c2564d3070441290029

```
--property parse.key=true \
--property key.separator=, \

kafka-avro-console-producer --broker-list kafka:9092 \
    --topic testing-avro \
    --property schema.registry.url=http://schema-registry:8081 \
    --property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
    --property "value.schema=$(tr -d '\n'<< EOF
    {"namespace": "solution.model",
        "type": "record",
        "name": "PositionValue",
        "fields": [
            {"name": "latitude", "type": "double"},
            {"name": "longitude", "type": "double"},
            {"name": "altitude", "type": "double", "default": 0.0}
        ]
    }
    EOF)"
{ "latitude": 51.5074, "longitude": 0.127, "altitude": 0.1 }


kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic testing-avro \
    --from-beginning
```

#### Kafka Connect

```
docker-compose up -d kafka zookeeper control-center populate 
docker-compose up -d schema-registry postgres connect 
```

View the postgres table

```
psql -U postgres -h postgres
select * from driver;
```

Create Connect connector

```
curl -s -X POST \
        -H "Content-Type: application/json" \
        --data '{
            "name": "Driver-Connector",
            "config": {
                "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
                "connection.url": "jdbc:postgresql://postgres:5432/postgres",
                "connection.user": "postgres",
                "table.whitelist": "driver",
                "topic.prefix": "",
                "mode":"timestamp+incrementing",
                "incrementing.column.name": "id",
                "timestamp.column.name": "timestamp",
                "table.types": "TABLE",
                "numeric.mapping": "best_fit",
                "transforms": "suffix,createKey,extractKey",
                "transforms.suffix.type":"org.apache.kafka.connect.transforms.RegexRouter",
                "transforms.suffix.regex":"(.*)",
                "transforms.suffix.replacement":"$1-avro",
                "transforms.createKey.type": "org.apache.kafka.connect.transforms.ValueToKey",
                "transforms.createKey.fields": "driverkey",
                "transforms.extractKey.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
                "transforms.extractKey.field": "driverkey"
            }
        }' http://connect:8083/connectors
```

Check the `driver` topic

```
kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```

Challenge: leave the `kafka-avro-console-consumer` running in a terminal window, and use `psql` to update a row (hint: `timestamp=now()`) in the driver table. What do you expect to see on the `driver-avro` topic? Can you find the `cleanup.policy` for the `driver-avro` topic? Why would this setting be used on the `driver-avro`?

#### Kafka Streams

```
docker-compose up -d kafka zookeeper control-center populate 
docker-compose up -d producer1 producer2 producer3 producer4
docker-compose up -d schema-registry webserver3
```

TODO: something to populate the `driver-avro` topic if the previous exercise was skipped.

Run the streams app. Check the topic is populated.

```
kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-distance-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```

Check `kafka-topics --bootstrap-server kafka:9092 --list`. What is the new topic `vp-streams-app-1-KSTREAM-AGGREGATE-STATE-STORE-0000000001-changelog`?

What is in the folder `/tmp/kafka-streams/vp-streams-app-1/`. (TODO: Double check the folder location on Ubuntu.)

In this exercise the topics `driver-avro` and `driver-positions-avro` are joined - what properties do the topics need in common for a successful join? (copartitioned: same key, same number of partitions, extra points: same partitioning algorithm.)


#### KSQL join

```
docker-compose up -d kafka zookeeper control-center populate 
docker-compose up -d producer1 producer2 producer3 producer4
docker-compose up -d schema-registry ksql-server webserver4
```

```
ksql http://ksql-server:8080

CREATE TABLE DRIVER (firstname VARCHAR, lastname VARCHAR, make VARCHAR, model VARCHAR) 
WITH (KAFKA_TOPIC='driver-avro', VALUE_FORMAT='avro');

SET 'auto.offset.reset' = 'earliest';

CREATE STREAM DRIVERPOSITIONS (latitude DOUBLE, longitude DOUBLE) 
WITH (kafka_topic='driver-positions-avro', value_format='avro');

CREATE STREAM driveraugmented 
WITH (kafka_topic='driver-augmented-avro', value_format='avro')
AS
SELECT 
  driverpositions.latitude, 
  driverpositions.longitude,
  driver.firstname,
  driver.lastname,
  driver.make,
  driver.model
FROM driverpositions 
LEFT JOIN driver on driverpositions.rowkey = driver.rowkey;
```


Check the `driver-augmented-avro` topic

```
kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-augmented-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```

#### KSQL fast-track


```
curl -X POST http://ksql-server:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE TABLE DRIVER (firstname VARCHAR, lastname VARCHAR, make VARCHAR, model VARCHAR) 
          WITH (KAFKA_TOPIC='driver-avro', VALUE_FORMAT='avro');"
        }
EOF

curl -X POST http://ksql-server:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE STREAM DRIVERPOSITIONS (latitude DOUBLE, longitude DOUBLE) 
          WITH (kafka_topic='driver-positions-avro', value_format='avro');"
        }
EOF

curl -X POST http://ksql-server:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE STREAM driveraugmented 
          WITH (kafka_topic='driver-augmented-avro', value_format='avro')
          AS
          SELECT 
            driverpositions.latitude, 
            driverpositions.longitude, 
            driver.firstname, 
            driver.lastname, 
            driver.make, 
            driver.model
          FROM driverpositions 
          LEFT JOIN driver on driverpositions.rowkey = driver.rowkey;"
        }
EOF
```
