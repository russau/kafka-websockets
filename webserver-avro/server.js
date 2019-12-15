const bootstrapServers = process.env.BOOTSTRAP_SERVERS || 'localhost:29092';
// /////// using node-rdkafka and avsc to do the desearialization myself
// const Kafka = require('node-rdkafka');
// const avro = require('avsc');
// const type = avro.Type.forSchema({
//   type: "record",
//   fields: [
//       {"name": "latitude", "type": "double"},
//       {"name": "longitude", "type": "double"}
//   ]
// });
// ///////
const KafkaAvro = require('kafka-avro');
const kafkaAvro = new KafkaAvro({
  kafkaBroker: bootstrapServers,
  schemaRegistry: 'http://schema-registry:8081',
});
kafkaAvro.init();
// ///////

const topics = [
  'driver-positions-avro',
];
let maxTopicIndex = 0;

// /////// doing the desearialization myself
// const stream = Kafka.createReadStream({
//   'group.id': 'webserver-01',
//   'metadata.broker.list': bootstrapServers,
// }, {'auto.offset.reset': 'earliest'}, {
//   topics: topics,
//   waitInterval: 0,
// });
//
// stream.on('data', function(data) {
//   const topicIndex = topics.indexOf(data.topic);
//   maxTopicIndex = Math.max(topicIndex, maxTopicIndex);
//
//   // https://github.com/mtth/avsc/issues/29#issuecomment-175196709
//   const val = type.decode(data.value, 5).value;
//   console.log(val.latitude, val.longitude);
//
//   if (topicIndex==maxTopicIndex) {
//     const arr = data.value.toString().split(',');
//     const message = {
//       'topic': data.topic,
//       'key': data.key.toString(),
//       'latitude': val.latitude,
//       'longitude': val.longitude,
//       'timestamp': data.timestamp,
//       'partition': data.partition,
//       'offset': data.offset,
//     };
//
//     io.sockets.emit('new message', message);
//   }
// });
// ///////

kafkaAvro.getConsumerStream({
  'group.id': 'webserver-01',
  'metadata.broker.list': bootstrapServers,
}, {'auto.offset.reset': 'earliest'}, {
  topics: topics,
  waitInterval: 0,
})
    .then(function(stream) {
      stream.on('data', function(data) {
        const topicIndex = topics.indexOf(data.topic);
        maxTopicIndex = Math.max(topicIndex, maxTopicIndex);

        if (topicIndex==maxTopicIndex) {
          const message = {
            'topic': data.topic,
            'key': data.key.toString(),
            'latitude': data.parsed.latitude,
            'longitude': data.parsed.longitude,
            'timestamp': data.timestamp,
            'partition': data.partition,
            'offset': data.offset,
          };

          io.sockets.emit('new message', message);
        }
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


