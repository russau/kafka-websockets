import geopy.distance
import json
import requests
import sys
import urllib.parse as urlparse
from urllib.parse import parse_qs
    
# https://stackoverflow.com/questions/31640252/get-latitude-and-longitude-points-along-a-line-between-two-points-by-percentage
def pointAtPercent(p0, p1, percent):
    if p0[0] != p1[0]:
        x = p0[0] + percent * (p1[0] - p0[0]);
    else:
        x = p0[0];

    if p0[1] != p1[1]:
        y = p0[1] + percent * (p1[1] - p0[1]);
    else:
        y = p0[1];

    return [x,y]
    
    
# curl 'https://www.gmap-pedometer.com/gp/ajaxRoute/get'  --data 'rId=7428398'
# http://www.gmap-pedometer.com?r=7428722
# http://www.gmap-pedometer.com?r=7429821
# http://www.gmap-pedometer.com?r=7429825
r = requests.post('https://www.gmap-pedometer.com/gp/ajaxRoute/get', data = {'rId':'7429825'})
polyline = parse_qs(r.text)['polyline'][0].split('a')
points = [[float(polyline[i]), float(polyline[i+1])] for i in range(0, len(polyline), 2)]
points.append(points[0])
velocity_points = []
stride = 10
distance_since_last_point = 0.0
remainder = 0 


i = 1
coords_1 = points[i-1]
coords_2 = points[i]
velocity_points.append(points[i])
while True:
    chop_up = geopy.distance.distance(coords_1, coords_2).m
    please_move = stride - distance_since_last_point
    if chop_up < please_move:
        distance_since_last_point += chop_up
        i += 1
        if i == len(points):
            break
        coords_1 = points[i-1]
        coords_2 = points[i]
    else:
        cut_at = please_move / chop_up
        coords_1 = pointAtPercent(coords_1, coords_2, cut_at)
        velocity_points.append(coords_1)
        distance_since_last_point = 0
        chop_up = geopy.distance.distance(coords_1, coords_2).m

velocity_points.append(points[-1]) 


print(json.dumps(velocity_points, indent=2))