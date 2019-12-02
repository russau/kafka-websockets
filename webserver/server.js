const Kafka = require('node-rdkafka');
var bootstrapServers = process.env.BOOTSTRAP_SERVERS || "localhost:29092";

var consumer = new Kafka.KafkaConsumer({
  'group.id': 'webserver-01',
  'metadata.broker.list': bootstrapServers,
}, {});

consumer.connect();

consumer
  .on('ready', function() {
    console.log("Consumer ready")
    consumer.subscribe(['driver-positions-distance']);
    consumer.consume();
  })
  .on('data', function(data) {
    
    var message = {
      "key" : data.key.toString(),
      "latitude" : data.value.toString().split(',')[0],
      "longitude" : data.value.toString().split(',')[1],      
      "timestamp" : data.timestamp,
      "latency" : new Date().getTime() - data.timestamp,
      "partition" : data.partition,
      "offset" : data.offset
    };
    
    if (data.value.toString().split(',').length > 2)
    {
      message['distace'] =  data.value.toString().split(',')[2];
    }
    
    io.sockets.emit('new message', message);
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