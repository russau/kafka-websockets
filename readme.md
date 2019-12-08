TODO

- `cat drivers.csv | kafka-console-producer --broker-list localhost:29092 --topic driver --property "key.separator=:" --property "parse.key=true"`
- Add a container to populate `drivers`, setup the ksql streams / table - OR should this be headlessly done. ``

  ```
  CREATE TABLE driver (name VARCHAR) with (KAFKA_TOPIC='driver', VALUE_FORMAT='DELIMITED');

  create stream driverpositions (lat varchar, lon varchar) with (kafka_topic='driver-positions', value_format='delimited');

  create stream driveraugmented 
  WITH (kafka_topic='driver-augmented', value_format='delimited')
  AS
  SELECT driverpositions.lat, driverpositions.lon, driver.name 
  from driverpositions LEFT JOIN driver on driverpositions.rowkey = driver.rowkey;
  ```

  ```
  curl -s -X POST \
         http://localhost:8088/ksql \
         -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
         -d '{
              "ksql": "CREATE TABLE driver (name VARCHAR) with (KAFKA_TOPIC='"'"'driver'"'"', VALUE_FORMAT='"'"'DELIMITED'"'"');"
              }'

  curl -s -X POST \
         http://localhost:8088/ksql \
         -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
         -d '{
              "ksql": "create stream driverpositions (lat varchar, lon varchar) with (kafka_topic='"'"'driver-positions'"'"', value_format='"'"'delimited'"'"');"
              }'

  curl -s -X POST \
         http://localhost:8088/ksql \
         -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
         -d '{
              "ksql": "create stream driveraugmented WITH (kafka_topic='"'"'driver-augmented'"'"', value_format='"'"'delimited'"'"') AS SELECT driverpositions.lat, driverpositions.lon, driver.name  from driverpositions LEFT JOIN driver on driverpositions.rowkey = driver.rowkey;"
              }'
  ```
