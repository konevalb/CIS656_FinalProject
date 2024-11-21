package server;

import java.io.*;
import java.net.*;
import java.util.logging.*;

class PeerHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(PeerHandler.class.getName());

    //Logger filehandler: see server-logs.txt for output (ignore.lck file, it is removed after closing the server)
    static {
        try {
            FileHandler fileHandler = new FileHandler("server-logs.txt", true); // Append mode
            fileHandler.setFormatter(new SimpleFormatter()); // Use a simple log format
            LOGGER.addHandler(fileHandler);

            Logger rootLogger = Logger.getLogger("");
            rootLogger.removeHandler(rootLogger.getHandlers()[0]);

            LOGGER.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to set up file handler for logger: " + e.getMessage());
        }
    }

    private final Socket peerSocket;

    public PeerHandler(Socket peerSocket) {
        this.peerSocket = peerSocket;
    }

    /**
     * Handles client connections and interactions for each <b>peer</b>.
     * <p>
     * This method registers the peer with the central server, provides it
     * with a random peer for connection (if available), and listens for
     * commands from the peer. Currently supported commands include:
     *  <ul>
     *     <li><b>"quit"</b>: Removes the peer from the network and terminates the connection.</li>
     * </ul>
     * <p>
     * Each instance of ClientHandler operates in its own thread, allowing
     * the central server to handle multiple peers concurrently.
     */
    @Override
    public void run() {
        InetSocketAddress peerAddress = new InetSocketAddress(peerSocket.getInetAddress(), peerSocket.getPort());

        try (BufferedReader input = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()))) {

            // Register the peer with the server
            Central_Server.addPeer(peerAddress);
            LOGGER.info("Peer joined: " + peerAddress);

            // Provide a random peer or status message
            InetSocketAddress randomPeer = Central_Server.getRandomPeer(peerAddress);
            if (randomPeer != null) {
                output.write("Connect to: " + randomPeer.getHostString() + " " + randomPeer.getPort() + "\n");
                LOGGER.info("Sent random peer to " + peerAddress + ": " + randomPeer);
            } else {
                output.write("You are the first peer in the network.\n");
                LOGGER.info("First peer in the network: " + peerAddress);
            }
            output.flush();

            // Handle commands from the peer
            String command;
            while ((command = input.readLine()) != null) {
                LOGGER.info("Received command from " + peerAddress + ": " + command);

                if (command.equalsIgnoreCase("quit")) {
                    LOGGER.info("Peer disconnecting: " + peerAddress);
                    Central_Server.removePeer(peerAddress);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error handling peer: " + peerAddress, e);
        } finally {
            try {
                peerSocket.close();
                LOGGER.info("Closed connection for peer: " + peerAddress);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing client socket for peer: " + peerAddress, e);
            }
        }
    }
}