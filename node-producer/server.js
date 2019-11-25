const Kafka = require('node-rdkafka');

var bootstrapServers = process.env.BOOTSTRAP_SERVERS || "localhost:9092";


const producer = new Kafka.Producer({
    'client.id': 'node-producer',
    'metadata.broker.list': bootstrapServers,
    'dr_cb': true
});

const fs = require('fs');
let rawdata = fs.readFileSync('drivers/1.js');
let coords = JSON.parse(rawdata);
let lasttime = Date.now();
let pos = 0;
  
producer.setPollInterval(100);
producer.connect();

producer.on('ready', () => {


  setInterval(() => {
    try {
        const key = "driver-1";
        const value = JSON.stringify(coords[pos]);
        producer.produce(
            'driver-positions',
            null,
            Buffer.from(value),
            key,
            Date.now()
        );
        pos = (pos + 1) % coords.length;
    } catch (err) {
        client.end(true);
        console.error('A problem occurred when sending our message');
        console.error(err);
    }    
  }, 1000)
});

producer.on('delivery-report', (err, report) => {
  // Report of delivery statistics here:
  console.log(report);
});

producer.on('event.error', err => {
    console.error('Error from producer');
    console.error(err);
})