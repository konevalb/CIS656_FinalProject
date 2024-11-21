#!/bin/bash

# Ensure the output directory for the Server module exists
mkdir -p ../out/production/Server

echo "Compiling the Server module..."
# Compile the server code
javac -d ../out/production/Server ../Server/src/server/*.java

echo "Starting the Server..."
# Run the server
java -cp ../out/production/Server server.Central_server