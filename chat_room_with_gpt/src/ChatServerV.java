import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * The Server class is responsible for managing the chat room application by hosting
 * an RMI server and registering remote objects for communication with clients.
 * It implements the ChatRoomServer interface to provide methods for clients to connect,
 * disconnect, and send messages to other clients and the GPT bot in the chat room.
 */
public class ChatServerV extends UnicastRemoteObject implements ChatServerInterface, Serializable  {
    private final List<ChatClientInterface> clients;
    private final List<ChatMessage> messages;
    private final LamportClock lamportClock;

    public ChatServerV() throws RemoteException {
        super();
        clients = new ArrayList<>();
        messages = new ArrayList<>();
        lamportClock = new LamportClock();
    }

     /**
     * Registers a new client to the database using two phase commit protocol.
     * @param client new client registering
     * @throws RemoteException thrown when remote invocation fails
     */
    public synchronized void register(ChatClientInterface client) throws IOException {
        DatabaseCoordinator databaseCoordinator = new DatabaseCoordinator();
        if (databaseCoordinator.twoPCInsertClient(client.getClientID(), client.getRoomID())) {
            clients.add(client);
        }
    }

    /**
     * Removes a client from the active clients list when they log out.
     * @param client client exiting the app
     * @throws RemoteException thrown when remote invocation fails
     */
    @Override
    public void removeClient(ChatClientInterface client) throws RemoteException {
        clients.remove(client);
    }


    /**
     * Broadcasts a message sent by a client to a room to all the clients who
     * are registered to that room.
     * @param message message entered by the client
     * @param c client who entered the message
     * @throws RemoteException thrown when remote invocation fails
     */
    public synchronized void broadcast(String message, ChatClientInterface c) throws IOException {

        ChatMessage chatMessage = new ChatMessage(c.getClientID(), c.getRoomID(), message);
        chatMessage.setTimestamp(lamportClock.tick());

        // Insert message into SQL database
        DatabaseCoordinator databaseCoordinator = new DatabaseCoordinator();
        if (databaseCoordinator.twoPCInsertMessage(chatMessage.getContent(),
                Integer.toString(chatMessage.getTimestamp()),
                chatMessage.getSender(), chatMessage.getRoom())) {
            messages.add(chatMessage);
        }

        for (ChatClientInterface client : clients) {
            if (client.getClientID().equals(c.getClientID()) || !Objects.equals(client.getRoomID(), c.getRoomID())) {
                continue;
            } else {
                client.receiveMessage(chatMessage);
            }
        }
    }

    /**
     * Gets a list of all the active clients in the chat application.
     * @return list of active clients
     * @throws RemoteException thrown when remote invocation fails
     */
    @Override
    public List<ChatClientInterface> getClients() throws RemoteException {
        return clients;
    }

    /**
     * Broadcasts the reply from ChatGPT for the prompt sent by a client
     * to a room to all the clients who are registered to that room.
     * @param message message entered by the client
     * @param c client who entered the message
     * @throws RemoteException thrown when remote invocation fails
     */
    public synchronized void broadcastGPTANS(String message, ChatClientInterface c) throws RemoteException {
        ChatMessage botAnswer = new ChatMessage("ChatGPT", c.getRoomID(), null);
        String answer;

        String cleanQuestion = message.replace("@BOT ", "");
        answer = OpenAIChatExample.getOpenAIResponse(cleanQuestion);
        botAnswer.setContent(answer);
        botAnswer.setTimestamp(lamportClock.tick());
        for (ChatClientInterface client : clients) {
            if (Objects.equals(client.getRoomID(), c.getRoomID())) {
                client.receiveAnswer(c, botAnswer);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream("chat_room_with_gpt/src/DBCred.properties");
            props.load(fis);
            String db1 = props.getProperty("db1");
            String db2 = props.getProperty("db2");

            // create the server object
            ChatServerV server = new ChatServerV();
            int port=Integer.parseInt(args[0]);
            // create the RMI registry and bind the server object to it
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("ChatServer", server);
            System.out.println("Chat server started");
            deleteDatabase(db1);
            deleteDatabase(db2);
        } catch (Exception e) {
            System.err.println("Chat server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteDatabase(String url) throws IOException {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream("chat_room_with_gpt/src/DBCred.properties");
            props.load(fis);
            String user = props.getProperty("username");
            String password = props.getProperty("password");

            String sql1 = "DELETE FROM messages";
            String sql2 = "DELETE FROM clients";
            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement stmt = conn.createStatement()) {
                int rowsAffectedInMessages = stmt.executeUpdate(sql1);
                System.out.println(rowsAffectedInMessages + " rows deleted from the messages table.");
                int rowsAffectedInClients = stmt.executeUpdate(sql2);
                System.out.println(rowsAffectedInClients + " rows deleted from the client table.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
}
