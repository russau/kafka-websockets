
- Add a container to populate `drivers`, setup the ksql streams / table - OR should this be headlessly done. ``

```
CREATE TABLE DRIVER (name VARCHAR) 
WITH (KAFKA_TOPIC='driver', VALUE_FORMAT='DELIMITED');

CREATE STREAM DRIVERPOSITIONS (lat VARCHAR, lon VARCHAR) 
WITH (kafka_topic='driver-positions', value_format='delimited');

create stream driveraugmented 
WITH (kafka_topic='driver-augmented', value_format='delimited')
AS
SELECT driverpositions.lat, driverpositions.lon, driver.name 
from driverpositions LEFT JOIN driver on driverpositions.rowkey = driver.rowkey;
```

```
curl -X POST http://localhost:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE TABLE DRIVER (name VARCHAR) 
                   WITH (KAFKA_TOPIC='driver', VALUE_FORMAT='DELIMITED');"
        }
EOF

curl -X POST http://localhost:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE STREAM DRIVERPOSITIONS (lat VARCHAR, lon VARCHAR) 
                   WITH (kafka_topic='driver-positions', value_format='delimited');"
        }
EOF


curl -X POST http://localhost:8088/ksql \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d @- << EOF | tr -d '\n'
        {
          "ksql": "CREATE STREAM DRIVERAUGMENTED
            WITH (kafka_topic='driver-augmented', value_format='delimited')
            AS
            SELECT
              driverpositions.lat,
              driverpositions.lon,
              driver.name
            FROM driverpositions 
            LEFT JOIN driver on driverpositions.rowkey = driver.rowkey;"
        }
EOF
```
