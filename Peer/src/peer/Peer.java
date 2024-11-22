package peer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Peer {
    private static final int serverPort = 9090; // Central server port
    //TODO explicitly name the serverIPAddress by prompting the user for input
    private static final String serverIPAddress = "127.0.0.1"; // Central server IP address
    private static int peerPort; // Port for this peer's own server
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Thread pool for peer connections
    private static final ConcurrentHashMap<InetSocketAddress, Socket> neighbors = new ConcurrentHashMap<>();
    private static volatile boolean isConnectedToServer = false; // Track connection status
    private static Socket serverSocket; // The connection to the central server
    private static BufferedReader serverInput;

    public static void main(String[] args) {
        try {
            // Allow peer to specify its own port in the terminal
            if (args.length > 0) {
                peerPort = Integer.parseInt(args[0]);
            } else {
                try (ServerSocket tempSocket = new ServerSocket(0)) {
                    peerPort = tempSocket.getLocalPort(); // Dynamically assign port
                }
                System.out.println("No port specified. Using dynamically assigned port: " + peerPort);
            }

            // Start the peer's own server
            new Thread(() -> startPeerServer(peerPort)).start();


            connectToServer();

            // user commands
            try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
                String command;
                while (true) {
                    System.out.print("Enter command (neighbors/quit/server status/reconnect): ");
                    command = userInput.readLine();

                    if (command.equalsIgnoreCase("quit")) {
                        disconnectFromServer();
                        disconnectFromNeighbors();
                        System.out.println("Disconnected from the network.");
                        break;
                    } else if (command.equalsIgnoreCase("neighbors")) {
                        displayNeighbors();
                    } else if (command.equalsIgnoreCase("server status")) {
                        checkServerStatus();
                    } else if (command.equalsIgnoreCase("reconnect")) {
                        reconnectToServer();
                    } else {
                        System.out.println("Unknown command. Available commands: neighbors, quit, server status, reconnect.");
                    }
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Attempts to connect to the central server.
     */
    private static synchronized void connectToServer() {
        try {
            serverSocket = new Socket(serverIPAddress, serverPort);
            serverInput = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            BufferedWriter serverOutput = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
            isConnectedToServer = true;
            System.out.println("Connected to the central server.");

            // Start a thread to listen for server messages
            new Thread(Peer::listenToServer).start();

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
        } catch (IOException e) {
            isConnectedToServer = false;
            System.out.println("Failed to connect to the central server.");
        }
    }

    /**
     * Listens for messages from the central server.
     */
    private static void listenToServer() {
        try {
            String message;
            while ((message = serverInput.readLine()) != null) {
                System.out.println("Message from server: " + message);
            }
        } catch (IOException e) {
            System.out.println("Connection to the central server lost!");
            isConnectedToServer = false;
        }
    }

    /**
     * Attempts to reconnect to the server upon user request.
     */
    private static void reconnectToServer() {
        if (isConnectedToServer) {
            System.out.println("Already connected to the server.");
            return;
        }
        System.out.println("Attempting to reconnect to the server...");
        connectToServer();
        if (isConnectedToServer) {
            System.out.println("Reconnected to the server.");
        } else {
            System.out.println("Failed to reconnect to the server.");
        }
    }

    /**
     * Disconnects from the central server.
     */
    private static void disconnectFromServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                isConnectedToServer = false;
                System.out.println("Disconnected from the central server.");
            }
        } catch (IOException e) {
            System.out.println("Error disconnecting from the server: " + e.getMessage());
        }
    }

    /**
     * Checks the server status and prints appropriate messages.
     */
    private static void checkServerStatus() {
        if (isConnectedToServer) {
            System.out.println("Server is up and you are connected!");
        } else {
            System.out.println("Server is down but you are still connected to your neighbors.");
            displayNeighbors();
        }
    }

    /**
     * Starts the peer's server to handle incoming peer connections.
     *
     * @param port The port number for the Peer's Server
     */
    private static void startPeerServer(int port) {
        try (ServerSocket peerServerSocket = new ServerSocket(port)) {
            System.out.println("Peer server is listening on port " + port);

            while (true) {
                Socket clientSocket = peerServerSocket.accept();
                threadPool.submit(() -> peerListener(clientSocket));
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
     * Listens for Peer connections requests and disconnects from peer upon receiving a disconnect request.
     *
     * @param peerSocket The peer socket for peer connections
     */
    private static void peerListener(Socket peerSocket) {
        InetSocketAddress remoteAddress = null;

        try (BufferedReader input = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()))) {

            remoteAddress = new InetSocketAddress(peerSocket.getInetAddress(), peerSocket.getPort());
            neighbors.put(remoteAddress, peerSocket);

            System.out.println("Connected to peer: " + remoteAddress);

            String message;
            while ((message = input.readLine()) != null) {
                System.out.println("Message from " + remoteAddress + ": " + message);
            }
        } catch (IOException e) {
            System.out.println("Peer disconnected: " + (remoteAddress != null ? remoteAddress : "Unknown peer"));
        } finally {
            if (remoteAddress != null) {
                try {
                    // Remove the neighbor and close the socket
                    Socket removedSocket = neighbors.remove(remoteAddress);
                    if (removedSocket != null && !removedSocket.isClosed()) {
                        removedSocket.close();
                        System.out.println("Closed socket for peer: " + remoteAddress);
                    }
                } catch (IOException e) {
                    System.out.println("Failed to close socket for peer: " + remoteAddress);
                }
                System.out.println("Removed disconnected peer: " + remoteAddress);
            }
        }
    }

    /**
     * Connects to a peer by attempting to establish a socket connection.
     * <p>redirects connecting peer to neighbor if neighbor allotment is full</p>
     *
     * @param host The host IP(name) for the peer we are connecting to
     * @param port The port number for the peer we are connecting to
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
                connectToPeer(redirectHost, redirectPort); //recursive call
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