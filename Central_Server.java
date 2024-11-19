import java.io.*;
import java.net.*;
import java.util.*;

public class Central_Server 
{
    static int serverPort = 9090;
    static List<InetSocketAddress> peerList = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) 
    {
        try 
        {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("The server is on!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch(IOException e) 
        {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable
{
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run()
    {
        try
        {
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

            InetSocketAddress peerAddress = new InetSocketAddress(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
            Central_Server.peerList.add(peerAddress);

            InetSocketAddress randomHostPeer = null;
            synchronized(Central_Server.peerList)
            {
                if(Central_Server.peerList.size() > 1)
                {
                    List<InetSocketAddress> copyList = new ArrayList<>(Central_Server.peerList);
                    copyList.remove(peerAddress);
                    randomHostPeer = copyList.get(new Random().nextInt(copyList.size()));
                }
            }

            clientSocket.close();

            if (randomHostPeer != null) 
            {
                System.out.println(randomHostPeer.getAddress().getHostAddress());
            } else 
            {
                System.out.println("No other peers available at the moment.");
            }

        } catch(Exception e) 
        {
            System.out.println("error!");
        } 
    }
}