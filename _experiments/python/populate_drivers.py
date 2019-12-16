"""
CREATE TABLE drivers (
    id SERIAL PRIMARY KEY,
    firstname varchar(50) not null,
    lastname varchar(50) not null,
    make varchar(50) not null,
    model varchar(50) not null,
    timestamp timestamp default current_timestamp
);
"""

import random

makemodel = [ ["Toyota", "Topic"],
["VW", "Broker"],
["Hyundai", "Replica"],
["GM", "Compaction"],
["Ford", "Insync"],
["Nissan", "Offset"],
["Honda", "Partition"] ]


from faker import Faker

drivers = []
drivers.extend([(Faker(['tr_TR']).first_name(), Faker(['tr_TR']).last_name()) for _ in range(2)])
drivers.extend([(Faker(['ja_JP']).first_name(), Faker(['ja_JP']).last_name()) for _ in range(2)])
drivers.extend([(Faker(['en_US']).first_name(), Faker(['en_US']).last_name()) for _ in range(10)])

random.shuffle(drivers)

for i, driver in enumerate(drivers):
    mm = random.choice(makemodel)
    print("""INSERT INTO driver (driverkey, firstname, lastname, make, model) VALUES
    ('driver-%s', '%s', '%s', '%s', '%s');
    """ % (i+1, driver[0], driver[1], mm[0], mm[1]))

