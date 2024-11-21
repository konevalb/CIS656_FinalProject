package peer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {
    private static final int serverPort = 9090; // Central server port
    private static final String serverIPAddress = "127.0.0.1"; // Central server IP address
    private static int peerPort; // Port for this peer's own server
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Thread pool for peer connections
    private static final ConcurrentHashMap<InetSocketAddress, Socket> neighbors = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            // Allow peer to specify its own port
            if (args.length > 0) {
                peerPort = Integer.parseInt(args[0]);
            } else {
                // Dynamically allocate a port if not specified
                try (ServerSocket tempSocket = new ServerSocket(0)) {
                    peerPort = tempSocket.getLocalPort(); // Get the dynamically assigned port
                }
                System.out.println("No port specified. Using dynamically assigned port: " + peerPort);
            }

            // Start the peer's own server
            new Thread(() -> startPeerServer(peerPort)).start();

            // Connect to the central server
            try (Socket serverSocket = new Socket(serverIPAddress, serverPort);
                 BufferedReader serverInput = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                 BufferedWriter serverOutput = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
                 BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

                System.out.println("Connected to the central server.");

                // Handle initial server response
                String response = serverInput.readLine();
                if (response.startsWith("Connect to:")) {
                    String[] parts = response.split(" ");
                    String peerHost = parts[2];
                    int peerPort = Integer.parseInt(parts[3]);
                    connectToPeer(peerHost, peerPort);
                } else {
                    System.out.println(response); // "You are the first peer in the network."
                }

                // Handle user commands
                String command;
                while (true) {
                    System.out.print("Enter command (neighbors/quit): ");
                    command = userInput.readLine();

                    if (command.equalsIgnoreCase("quit")) {
                        // Notify the central server and close the connection
                        serverOutput.write("quit\n");
                        serverOutput.flush();
                        System.out.println("Notified the server of disconnection.");

                        // Disconnect from the server
                        serverSocket.close();
                        System.out.println("Disconnected from the server.");

                        // Disconnect from all neighbors
                        disconnectFromNeighbors();

                        System.out.println("Disconnected from the network.");
                        break;
                    } else if (command.equalsIgnoreCase("neighbors")) {
                        displayNeighbors();
                    } else {
                        System.out.println("Unknown command. Available commands: neighbors, quit.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Unable to connect to the central server. The server may be down.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Starts the peer's server to handle incoming peer connections.
     */
    private static void startPeerServer(int port) {
        try (ServerSocket peerServerSocket = new ServerSocket(port)) {
            System.out.println("Peer server is listening on port " + port);

            while (true) {
                Socket clientSocket = peerServerSocket.accept();
                threadPool.submit(() -> handleIncomingConnection(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("Error starting peer server: " + e.getMessage());
        }
    }

    /**
     * Disconnects from all neighbors.
     */
    private static void disconnectFromNeighbors() {
        for (InetSocketAddress neighbor : neighbors.keySet()) {
            try (Socket neighborSocket = neighbors.remove(neighbor);
                 BufferedWriter neighborOutput = new BufferedWriter(new OutputStreamWriter(neighborSocket.getOutputStream()))) {

                neighborOutput.write(": Peer " + peerPort + " disconnected!\n");
                neighborOutput.flush();
                System.out.println("Notified neighbor: " + neighbor);
            } catch (IOException e) {
                System.out.println("Failed to notify neighbor: " + neighbor);
            }
        }
    }

    /**
     * Handles incoming peer connection requests.
     */
    private static void handleIncomingConnection(Socket clientSocket) {
        InetSocketAddress remoteAddress = null;

        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            remoteAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
            neighbors.put(remoteAddress, clientSocket);

            System.out.println("Connected to peer: " + remoteAddress);

            String message;
            while ((message = input.readLine()) != null) {
                System.out.println("Message from " + remoteAddress + ": " + message);
            }
        } catch (IOException e) {
            System.out.println("Peer disconnected: " + (remoteAddress != null ? remoteAddress : "Unknown peer"));
        } finally {
            if (remoteAddress != null) {
                neighbors.remove(remoteAddress);
                System.out.println("Removed disconnected peer: " + remoteAddress);
            }
        }
    }

    /**
     * Connects to a peer by attempting to establish a socket connection.
     */
    private static void connectToPeer(String host, int port) {
        try (Socket peerSocket = new Socket(host, port);
             BufferedReader peerInput = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
             BufferedWriter peerOutput = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()))) {

            System.out.println("Connecting to peer: " + host + ":" + port);

            String response = peerInput.readLine();
            if (response.startsWith("Accepted connection")) {
                System.out.println("Successfully connected to peer: " + host + ":" + port);
                InetSocketAddress peerAddress = new InetSocketAddress(host, port);
                neighbors.put(peerAddress, peerSocket);
            } else if (response.startsWith("Redirect to:")) {
                String[] parts = response.split(" ");
                String redirectHost = parts[2];
                int redirectPort = Integer.parseInt(parts[3]);
                System.out.println("Redirected to peer: " + redirectHost + ":" + redirectPort);
                connectToPeer(redirectHost, redirectPort);
            }
        } catch (IOException e) {
            System.out.println("Error connecting to peer: " + host + ":" + port);
        }
    }

    /**
     * Displays the current neighbors of this peer.
     */
    private static void displayNeighbors() {
        if (neighbors.isEmpty()) {
            System.out.println("No neighbors connected.");
        } else {
            System.out.println("Your neighbors:");
            for (InetSocketAddress neighbor : neighbors.keySet()) {
                System.out.println("- " + neighbor);
            }
        }
    }
}