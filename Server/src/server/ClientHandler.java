package server;

import java.io.*;
import java.net.*;

class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        InetSocketAddress peerAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());

        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            // Register the peer with the server
            Central_Server.addPeer(peerAddress);
            System.out.println("Peer joined: " + peerAddress);

            // Provide a random peer or status message
            InetSocketAddress randomPeer = Central_Server.getRandomPeer(peerAddress);
            if (randomPeer != null) {
                output.write("Connect to: " + randomPeer.getHostString() + " " + randomPeer.getPort() + "\n");
            } else {
                output.write("You are the first peer in the network.\n");
            }
            output.flush();

            // Handle commands from the peer
            String command;
            while ((command = input.readLine()) != null) {
                if (command.equalsIgnoreCase("quit")) {
                    System.out.println("Peer disconnecting: " + peerAddress);
                    Central_Server.removePeer(peerAddress);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling peer: " + peerAddress);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}