import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.sql.*;

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


    public synchronized void register(ChatClientInterface client) throws RemoteException {
        DatabaseCoordinator databaseCoordinator = new DatabaseCoordinator();
        if (databaseCoordinator.twoPCInsertClient(client.getClientID(), client.getRoomID())) {
            clients.add(client);
        }
    }

    @Override
    public void removeClient(ChatClientInterface client) throws RemoteException {
        clients.remove(client);
    }


    public synchronized void broadcast(String message, ChatClientInterface c) throws RemoteException {
        System.out.println("Broadcast message: " + message);
        System.out.println("Broadcast client: " + c.getClientID() + " from room ID: " + c.getRoomID());

        ChatMessage chatMessage = new ChatMessage(c.getClientID(), c.getRoomID(), message);
        chatMessage.setTimestamp(lamportClock.tick());

        // Insert message into SQL database
        DatabaseCoordinator databaseCoordinator = new DatabaseCoordinator();
        if (databaseCoordinator.twoPCInsertMessage(chatMessage.getContent(),
                Integer.toString(chatMessage.getTimestamp()),
                chatMessage.getSender(), chatMessage.getRoom())) {
            messages.add(chatMessage);
            System.out.println("Message inserted into SQL database");
        } else {
            System.err.println("Error inserting message into SQL database");
        }

        for (ChatClientInterface client : clients) {
            System.out.println("In side for loop broadcast client: " + client.getClientID() + " from room ID: "
                    + client.getRoomID());
            if (client.getClientID().equals(c.getClientID()) || !Objects.equals(client.getRoomID(), c.getRoomID())) {
                System.out.println("continue");
                continue;
            } else {
                client.receiveMessage(chatMessage);
            }
        }
    }


    @Override
    public List<ChatClientInterface> getClients() throws RemoteException {
        return clients;
    }

    public synchronized void broadcastGPTANS(String message, ChatClientInterface c) throws RemoteException {
        ChatMessage botAnswer = new ChatMessage("ChatGPT", c.getRoomID(), null);
        String answer;

        // I need to get rid of "@BOT" in the message
        String cleanQuestion = message.replace("@BOT ", "");
        System.out.println(cleanQuestion);
        answer = OpenAIChatExample.getOpenAIResponse(cleanQuestion);
        System.out.println(answer);
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
            // create the server object
            ChatServerV server = new ChatServerV();
            // create the RMI registry and bind the server object to it
            Registry registry = LocateRegistry.createRegistry(3000);
            registry.rebind("ChatServer", server);
            System.out.println("Chat server started");
        } catch (Exception e) {
            System.err.println("Chat server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
