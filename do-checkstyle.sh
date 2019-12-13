#!/bin/bash
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml java-producer/src/
java -jar checkstyle/checkstyle-8.24-all.jar -c checkstyle/google_checks.xml streams/src/