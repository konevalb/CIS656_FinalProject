#!/bin/bash

# Ensure the output directory for the Peer module exists
mkdir -p ../out/production/Peer

# Checking if the Peer class exists
if [ ! -f ../out/production/Peer/peer/Peer.class ]; then
    echo "Compiling the Peer module..."
    # Compile the Peer module only if the compiled class files don't exist
    javac -d ../out/production/Peer ../Peer/src/peer/*.java
else
    echo "Peer client is already compiled."
fi

echo "Running the Peer client..."
# Run the Peer client
java -cp ../out/production/Peer peer.Peer