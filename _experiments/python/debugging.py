import geopy.distance
import json
import sys

with open('/Users/russell.sayers/Documents/TestingZone/first-leaflet/node-producer/drivers/1.js') as json_file:
    data = json.load(json_file)

for i in range(0, len(data), 2):
    dist = geopy.distance.distance(data[i], data[i+1]).m
    print(data[i], data[i+1], dist)