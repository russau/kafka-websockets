const Kafka = require('node-rdkafka');
var bootstrapServers = process.env.BOOTSTRAP_SERVERS || "localhost:9092";

var consumer = new Kafka.KafkaConsumer({
  'group.id': 'webserver-01',
  'metadata.broker.list': bootstrapServers,
}, {});

consumer.connect();

consumer
  .on('ready', function() {
    consumer.subscribe(['driver-positions']);
    consumer.consume();
  })
  .on('data', function(data) {
    // Output the actual message contents
    console.log(".");
    io.sockets.emit('new message', data.value.toString());
  });
  

// Setup basic express server
var express = require('express');
var app = express();
var path = require('path');
var server = require('http').createServer(app);
var io = require ('socket.io') (server);
var port = process.env.PORT || 3000;

server.listen(port, () => {
  console.log('Server listening at port %d', port);
});

// Routing
app.use(express.static(path.join(__dirname, 'public')));

// log when we get a websocket connection
io.on('connection', (socket) => {
  console.log('new connection, socket.id: ' + socket.id);
});