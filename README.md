# CIS 656 Final: Peer-to-Peer Network with Central Server 

This application implements a distributed peer-to-peer (P2P) system with a central server for peer registration and neighbor assignment. The system is designed to demonstrate communication, concurrency, and P2P architecture.

---

## **Features**

### **Central Server**
- **Peer Registration:**  
  Peers register themselves to the central server, which assigns them to an existing peer in the network if available.
- **Neighbor Management:**  
  Tracks connected peers and assigns a random peer to newly joining peers.
- **Commands:**
    - `members`: Displays a list of all currently connected peers.
    - `quit`: Shuts down the central server (no new peers can join afterward).
- **Note:**  
  The Server does not track the topology of the peer network at the moment. Upon subsequent reconnects from Peers they are not saved to the internal List of Peers.

---

### **Peer**
- **Connect to Server:**  
  Peers connect to the central server to join the network.
- **Neighbor Management:**  
  Peers manage their direct (one-hop) neighbors, allowing a maximum of 3 neighbors.
- **Redirection Logic:**  
  If a peer is full (has 3 neighbors), it redirects new connections to one of its existing neighbors.
- **Commands:**
    - `neighbors`: Displays the peer's current neighbors.
    - `server status`: Checks if the peer is connected to the central server and displays the server status.
    - `reconnect`: Attempts to reconnect to the central server if the connection is lost.
    - `quit`: Disconnects from the server and all neighbors, notifying them of the disconnection.

---

## **How to Run**

### **Prerequisites**
- Java Development Kit (JDK) 8 or higher.
- IntelliJ IDEA or any other Java IDE (optional but recommended).

---

### **Running the Central Server and Peer**

1. Compiling and running the server:
   ```bash
   bash scripts/run_server.sh
   
2. Compile the peer:
   ```bash
   bash scripts/run_peer.sh
3. If you would like to specify the peer port 
   ```bash
   bash scripts/run_peer.sh 5001
