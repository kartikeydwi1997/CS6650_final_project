import java.rmi.*;

public interface ChatClientInterface extends Remote {
    void receiveMessage(ChatMessage message) throws RemoteException;

    void sendMessage(String message, MessageCallback messageCallback) throws RemoteException;

    String getClientID() throws RemoteException;

    String getRoomID() throws RemoteException;

    ChatServerInterface getServer() throws RemoteException;
}