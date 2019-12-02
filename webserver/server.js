const Kafka = require('node-rdkafka');
const bootstrapServers = process.env.BOOTSTRAP_SERVERS || 'localhost:29092';

const consumer = new Kafka.KafkaConsumer({
  'group.id': 'webserver-01',
  'metadata.broker.list': bootstrapServers,
}, {});

consumer.connect();

consumer
    .on('ready', function() {
      console.log('Consumer ready');
      consumer.subscribe(['driver-positions-distance']);
      consumer.consume();
    })
    .on('data', function(data) {
      const arr = data.value.toString().split(',');
      const message = {
        'key': data.key.toString(),
        'latitude': parseFloat(arr[0]).toFixed(6),
        'longitude': parseFloat(arr[1]).toFixed(6),
        'timestamp': data.timestamp,
        // "latency" : new Date().getTime() - data.timestamp,
        'partition': data.partition,
        'offset': data.offset,
      };

      if (arr.length > 2) {
        message['distance'] = Math.round(arr[2]);
      }

      io.sockets.emit('new message', message);
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
