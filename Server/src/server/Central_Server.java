package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Central_Server {
    static int serverPort = 9090;
    //thread-safe ArrayList
    public static final List<InetSocketAddress> peerList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("The Server is on!");
            System.out.println("See server-logs.txt for logs");

            // Start a thread to listen for terminal commands
            new Thread(() -> handleServerCommands(serverSocket)).start();

            // Main server loop to handle peer connections
            while (!serverSocket.isClosed()) {
                try {
                    Socket PeerSocket = serverSocket.accept();
                    new Thread(new PeerHandler(PeerSocket)).start();
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("Server shutting down...");
                        break; // Exit the loop when the server socket is closed
                    } else {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if ("Socket closed".equalsIgnoreCase(e.getMessage())) {
                System.out.println("Server socket closed. Shutting down...");
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles terminal commands <b>server side</b> (members/quit).
     *
     * @param serverSocket The main server socket.
     */
    private static void handleServerCommands(ServerSocket serverSocket) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter command (members/quit): ");
                String command = scanner.nextLine().trim();

                if (command.equalsIgnoreCase("members")) {
                    // List all connected peers
                    if (peerList.isEmpty()) {
                        System.out.println("No peers are currently connected.");
                    } else {
                        System.out.println("Connected peers:");
                        for (InetSocketAddress peer : peerList) {
                            System.out.println("- " + peer);
                        }
                    }
                } else if (command.equalsIgnoreCase("quit")) {
                    // Shut down the server
                    System.out.println("Shutting down the server...");
                    serverSocket.close(); // Stop accepting new connections
                    System.exit(0); // Exit the program
                } else {
                    System.out.println("Unknown command. Available commands: members, quit.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling server commands: " + e.getMessage());
        }
    }

    /**
     * Adds a peer to the list of connected peers.
     *
     * @param peerAddress The address of the peer to add.
     */
    public static synchronized void addPeer(InetSocketAddress peerAddress) {
        if (!peerList.contains(peerAddress)) {
            peerList.add(peerAddress);
            System.out.println("Peer added: " + peerAddress);
        }
    }

    /**
     * Removes a peer from the list of connected peers.
     *
     * @param peerAddress The address of the peer to remove.
     */
    public static synchronized void removePeer(InetSocketAddress peerAddress) {
        if (peerList.remove(peerAddress)) {
            System.out.println("Peer removed: " + peerAddress);
        }
    }

    /**
     * Returns a random peer address from the list of connected peers, excluding a specific peer.
     *
     * @param excludingPeer The peer to exclude from the random selection.
     * @return A random peer address, or null if no other peers are available.
     */
    public static synchronized InetSocketAddress getRandomPeer(InetSocketAddress excludingPeer) {
        List<InetSocketAddress> availablePeers = new ArrayList<>(peerList);
        availablePeers.remove(excludingPeer); // Excluding the requesting peer

        if (availablePeers.isEmpty()) {
            return null;
        }

        Random random = new Random();
        return availablePeers.get(random.nextInt(availablePeers.size()));
    }
}