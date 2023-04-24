import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;

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


    @Override
    public List<ChatClientInterface> getClients() throws RemoteException {
        return clients;
    }

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
