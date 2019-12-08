FROM node:dubnium-stretch
RUN apt-get update && \
apt-get install -y kafkacat
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .


