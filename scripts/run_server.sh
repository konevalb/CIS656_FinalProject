#!/bin/bash

# Set the working directory to the script's location
cd "$(dirname "$0")" || exit 1

# Ensure the output directory exists
mkdir -p ../out/production/Server

# Compile the Server module
echo "Compiling the Server module..."
javac -d ../out/production/Server ../Server/src/server/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed. Check the paths or syntax in your code."
    exit 1
fi

# Start the Server
echo "Starting the Server..."
java -cp ../out/production/Server server.Central_Server
if [ $? -ne 0 ]; then
    echo "Failed to start the server. Check your classpath or main class."
    exit 1
fi