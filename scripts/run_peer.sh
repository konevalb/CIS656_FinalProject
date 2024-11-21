#!/bin/bash

# Change to the script's directory or exit if it fails
cd "$(dirname "$0")" || exit 1

# Ensure the output directory for the Peer module exists
mkdir -p ../out/production/Peer

# Check if the Peer class exists
if [ ! -f ../out/production/Peer/peer/Peer.class ]; then
    echo "Compiling the Peer module..."
    # Compile the Peer module only if the compiled class files don't exist
    javac -d ../out/production/Peer ../Peer/src/peer/*.java
    if [ $? -ne 0 ]; then
        echo "Compilation failed. Check the paths or syntax in your code."
        exit 1
    fi
else
    echo "Peer client is already compiled."
fi

# Run the Peer client
echo "Running the Peer client..."
java -cp ../out/production/Peer peer.Peer
if [ $? -ne 0 ]; then
    echo "Failed to start the Peer client. Check your classpath or main class."
    exit 1
fi