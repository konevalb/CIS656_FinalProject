import java.io.*;
import java.net.*;

public class Peer 
{
    private static final int serverPort = 9090;
    private static final String serverIPAddress = "127.0.0.1";

    public static void main(String[] args) 
    {
        try 
        {
            Socket serverSocket = new Socket(serverIPAddress, serverPort);
            BufferedReader serverInput = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        } catch(IOException e) 
        {
            e.printStackTrace();
        }
    }
}