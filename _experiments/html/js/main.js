



var mymap = L.map('mapid').setView([47.610664, -122.338917
            ], 13);
L.tileLayer('https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}{r}.{ext}', {
 ext: 'png',
 maxZoom: 18,
 attribution: 'Wikimedia maps | Map data &copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap contributors</a>'
}).addTo(mymap);

for (const p in velocity_points) {
  L.marker(velocity_points[p]).addTo(mymap);
}

var greenIcon = new L.Icon({
  iconUrl: 'https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});


