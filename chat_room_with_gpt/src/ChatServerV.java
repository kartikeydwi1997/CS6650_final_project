import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.sql.*;

public class ChatServerV extends UnicastRemoteObject implements ChatServerInterface {
    private List<ChatClientInterface> clients;

    public ChatServerV() throws RemoteException {
        super();
        clients = new ArrayList<ChatClientInterface>();
    }

    public synchronized void register(ChatClientInterface client) throws RemoteException {
        DatabaseCoordinator databaseCoordinator = new DatabaseCoordinator();
        if (databaseCoordinator.twoPCInsertClient(client.getClientID())) {
            clients.add(client);
        }
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

//    private void saveClientToDatabase(String clientID) {
//        DatabaseConnector databaseConnector = new DatabaseConnector();
//        try {
//            databaseConnector.twoPCInsertClient(clientID);
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

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
