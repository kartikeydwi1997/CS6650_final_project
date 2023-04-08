import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.sql.*;

public class ChatServerV extends UnicastRemoteObject implements ChatServerInterface {
    private List<ChatClientInterface> clients;
    private Connection conn;

    public ChatServerV() throws RemoteException {
        super();
        clients = new ArrayList<ChatClientInterface>();
        connectToDatabase();
    }

    public synchronized void register(ChatClientInterface client) throws RemoteException {
        clients.add(client);
        saveClientToDatabase(client.getClientID());
    }

    public synchronized void broadcast(String message, ChatClientInterface c) throws RemoteException {
        System.out.println(message);
        for (ChatClientInterface client : clients) {
            if (client.getClientID().equals(c.getClientID())) {
                continue;
            } else {
                client.receiveMessage(c, message);
            }

        }
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/ds_final";
            String user = "root";
            String password = "The1isyou";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveClientToDatabase(String clientID) {
        try {
            Statement stmt = conn.createStatement();
            String sql = "INSERT INTO clients (client_id) VALUES ('" + clientID + "')";
            stmt.executeUpdate(sql);
            stmt.close();
            System.out.println("Client ID saved to database: " + clientID);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // create the server object
            ChatServerV server = new ChatServerV();

            // create the RMI registry and bind the server object to it
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ChatServer", server);

            System.out.println("Chat server started");
        } catch (Exception e) {
            System.err.println("Chat server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
