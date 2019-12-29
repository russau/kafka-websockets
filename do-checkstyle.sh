#!/bin/bash -ex
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml java-consumer/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml java-consumer-avro/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml java-consumer-prev/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml java-producer/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml java-producer-avro/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml streams/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml streams-avro/src/
pushd webserver ; npm run pretest ; popd
pushd webserver-avro ; npm run pretest ; popd
pushd node-producer ; npm run pretest ; popd
pushd python-consumer ; python3 -m pylint main.py ; popd
pushd python-producer ; python3 -m pylint main.py ; popd
pushd python-producer-avro ; python3 -m pylint main.py ; popd
pushd dotnet-producer ; dotnet build  --no-incremental ; popd
pushd dotnet-producer-avro ; dotnet build  --no-incremental ; popd