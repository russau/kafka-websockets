const Kafka = require('node-rdkafka');
const topics = [
  'driver-positions', 'driver-positions-distance', 'driver-augmented',
];
let maxTopicIndex = 0;
const stream = Kafka.createReadStream({
  'group.id': 'webserver-01',
  'metadata.broker.list': 'kafka:9092',
}, {'auto.offset.reset': 'earliest'}, {
  topics: topics,
  waitInterval: 0,
});

stream.on('data', function(data) {
  const topicIndex = topics.indexOf(data.topic);
  maxTopicIndex = Math.max(topicIndex, maxTopicIndex);

  if (topicIndex==maxTopicIndex) {
    const arr = data.value.toString().split(',');
    const message = {
      'topic': data.topic,
      'key': data.key.toString(),
      'latitude': parseFloat(arr[0]).toFixed(6),
      'longitude': parseFloat(arr[1]).toFixed(6),
      'timestamp': data.timestamp,
      // "latency" : new Date().getTime() - data.timestamp,
      'partition': data.partition,
      'offset': data.offset,
    };

    if (data.topic == 'driver-positions-distance') {
      message['distance'] = Math.round(arr[2]);
    }

    if (data.topic == 'driver-augmented') {
      message['driver'] = arr[2];
    }

    io.sockets.emit('new message', message);
  }
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
