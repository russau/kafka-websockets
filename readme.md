### Create Connect connector

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
                "transforms": "createKey,extractKey",
                "transforms.createKey.type": "org.apache.kafka.connect.transforms.ValueToKey",
                "transforms.createKey.fields": "driverkey",
                "transforms.extractKey.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
                "transforms.extractKey.field": "driverkey"
            }
        }' http://localhost:8083/connectors
```

### Check the `driver` topic

```
kafka-avro-console-consumer --bootstrap-server localhost:29092 \
    --property schema.registry.url=http://localhost:8081 \
    --topic driver --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```

### Create KSQL streams / tables

```
CREATE TABLE DRIVER (firstname VARCHAR, lastname VARCHAR, make VARCHAR, model VARCHAR) 
WITH (KAFKA_TOPIC='driver', VALUE_FORMAT='avro');

SET 'auto.offset.reset' = 'earliest';

CREATE STREAM DRIVERPOSITIONS (latitude DOUBLE, longitude DOUBLE) 
WITH (kafka_topic='driver-positions', value_format='avro');

CREATE STREAM driveraugmented 
WITH (kafka_topic='driver-augmented', value_format='avro')
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

```
curl -X POST http://localhost:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE TABLE DRIVER (firstname VARCHAR, lastname VARCHAR, make VARCHAR, model VARCHAR) 
          WITH (KAFKA_TOPIC='driver', VALUE_FORMAT='avro');"
        }
EOF

curl -X POST http://localhost:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE STREAM DRIVERPOSITIONS (latitude DOUBLE, longitude DOUBLE) 
          WITH (kafka_topic='driver-positions', value_format='avro');"
        }
EOF

curl -X POST http://localhost:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE STREAM driveraugmented 
          WITH (kafka_topic='driver-augmented', value_format='avro')
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

### Check the `driver-augmented` topic

```
kafka-avro-console-consumer --bootstrap-server localhost:29092 \
    --property schema.registry.url=http://localhost:8081 \
    --topic driver-augmented --property print.key=true \
    --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --from-beginning
```
