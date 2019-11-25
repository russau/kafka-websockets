import json
import fileinput

for line in fileinput.input():
    obj = json.loads(line)
    coords = [[c[1],c[0]] for c in obj["routes"][0]["geometry"]["coordinates"]]
    print(json.dumps(coords, indent=2))

