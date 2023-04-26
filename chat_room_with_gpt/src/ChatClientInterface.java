import java.io.IOException;
import java.rmi.*;

/**
 * Defines the methods for a client using the chat room
 * application. A client can log in to the app using their username
 * and enter a room to start chatting in. A user is allowed to send
 * a message to those who are active in the room. A user can also
 * prompt GPT for a response using @BOT before their message.
 */
public interface ChatClientInterface extends Remote {
    /**
     * Receive messages sent by other clients in the same room
     * from the server.
     * @param message message sent by other clients
     * @throws RemoteException thrown when remote invocation fails.
     */
    void receiveMessage(ChatMessage message) throws RemoteException;

     /**
     * Sends message to the server.
     * @param message message sent by current client
     * @throws RemoteException thrown when remote invocation fails.
     */
    void sendMessage(String message) throws IOException;

    /**
     * Get the client id of current user.
     * @return client id
     * @throws RemoteException thrown when remote invocation fails.
     */
    String getClientID() throws RemoteException;

    /**
     * Get the room id where the current client is registered
     * @return room id
     * @throws RemoteException thrown when remote invocation fails.
     */
    String getRoomID() throws RemoteException;

    /**
     * Disconnects the client from the application
     * @throws RemoteException thrown when remote invocation fails.
     */
    void exitApp() throws RemoteException;

    /**
     * Receive ChatGPT answers replied to questions sent by other
     * clients in the same room from the server.
     * @param c client id prompting GPT
     * @param message Prompt made by the client
     * @throws RemoteException thrown when remote invocation fails.
     */
    void receiveAnswer(ChatClientInterface c, ChatMessage message) throws RemoteException;
}