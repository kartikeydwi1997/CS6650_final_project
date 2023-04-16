import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;


// This class implements the ChatClientInterface, which is the remote interface
// that clients use to receive messages from the server.
class ChatClientImpl extends UnicastRemoteObject implements ChatClientInterface {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;
    private final String ClientID;
    private final String RoomID;
    private final LamportClock lamportClock;

    public ChatClientImpl(String cid, String roomID) throws RemoteException {
        this.ClientID = cid;
        this.RoomID = roomID;
        this.lamportClock = new LamportClock();
    }

    public String getClientID() {
        return this.ClientID;
    }

    public String getRoomID() {
        return this.RoomID;
    }


    public void receiveMessage(ChatClientInterface c, ChatMessage message) throws RemoteException {
        lamportClock.update(message.getTimestamp());
        System.out.println("Timestamp: " + message.getTimestamp());
        System.out.println("Client " + c.getClientID() + " from room ID:"+ c.getRoomID()+ ": " + message.getContent());
    }

    public static void main(String[] args) {
        try {
            // lookup the remote chat server object
            ChatServerInterface server = (ChatServerInterface) Naming
                    .lookup("rmi://" + SERVER_HOST + ":" + SERVER_PORT + "/ChatServer");

            // create the client object and register with the server
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.print("Enter client ID: ");
            String clientID = scanner.nextLine();
            System.out.println("Enter room ID: ");
            String roomID = scanner.nextLine();
            ChatClientInterface client = new ChatClientImpl(clientID, roomID);
            server.register(client);

            // read user input and send messages to the server
            while (true) {
                String message = scanner.nextLine();
                server.broadcast(message, client);
            }
        } catch (Exception e) {
            System.err.println("Chat client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
