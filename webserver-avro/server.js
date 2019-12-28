const KafkaAvro = require('kafka-avro');
const kafkaAvro = new KafkaAvro({
  kafkaBroker: 'kafka:9092',
  schemaRegistry: 'http://schema-registry:8081',
});
kafkaAvro.init()
    .then(function() {
      console.log('Ready to use');
    });

const topics = [
  process.env.TOPIC,
];

console.log('subscribing to ', topics);
kafkaAvro.getConsumerStream({
  'group.id': process.env.HOSTNAME,
  'metadata.broker.list': 'kafka:9092',
}, {'auto.offset.reset': 'earliest'}, {
  topics: topics,
  waitInterval: 0,
})
    .then(function(stream) {
      stream.on('data', function(data) {
        const message = {
          'topic': data.topic,
          'key': data.key.toString(),
          'timestamp': data.timestamp,
          'partition': data.partition,
          'offset': data.offset,
        };

        message['latitude'] = data.parsed.latitude ||
          data.parsed.LATITUDE.double;
        message['longitude'] = data.parsed.longitude ||
          data.parsed.LONGITUDE.double;

        if (data.topic == 'driver-distance-avro') {
          message['distance'] = Math.round(data.parsed.distance);
        }

        // different format for ksql avro stream
        if (data.topic == 'driver-augmented-avro') {
          message['firstname'] = data.parsed.FIRSTNAME ?
             data.parsed.FIRSTNAME.string : null;
          message['lastname'] = data.parsed.LASTNAME ?
             data.parsed.LASTNAME.string : null;
          message['make'] = data.parsed.MAKE ?
              data.parsed.MAKE.string : null;
          message['model'] = data.parsed.MODEL ?
              data.parsed.MODEL.string : null;
        }
        io.sockets.emit('new message', message);
      });
    });

// Setup basic express server
const express = require('express');
const app = express();
const path = require('path');
const server = require('http').createServer(app);
const io = require('socket.io')(server);
const port = process.env.PORT || 3000;

server.listen(port, () => {
  console.log('Server listening at port %d', port);
});

// Routing
app.use(express.static(path.join(__dirname, 'public')));

// log when we get a websocket connection
io.on('connection', (socket) => {
  console.log('new connection, socket.id: ' + socket.id);
});

