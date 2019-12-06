# kafka-console-producer --broker-list localhost:29092 --topic driver --property "key.separator=:" --property "parse.key=true"

from faker import Faker

faker = Faker(['tr_TR', 'en_US', 'ja_JP'])

for i in range(100):
    print("driver-%s:%s" % (i, faker.name()))