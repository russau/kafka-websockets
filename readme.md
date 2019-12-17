### Create Connect connector

If you are running this on your laptop add these entries to your `/etc/hosts` file.

```
127.0.0.1 kafka
127.0.0.1 schema-registry
127.0.0.1 connect
127.0.0.1 ksql-server
```

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

### Check the `driver` topic

```
kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-positions-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning

kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```

### Create KSQL streams / tables

```
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
JOIN driver on driverpositions.rowkey = driver.rowkey;
```

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
          JOIN driver on driverpositions.rowkey = driver.rowkey;"
        }
EOF
```

### Check the `driver-augmented-avro` topic

```
kafka-avro-console-consumer --bootstrap-server kafka:9092 \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic driver-augmented-avro --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```
