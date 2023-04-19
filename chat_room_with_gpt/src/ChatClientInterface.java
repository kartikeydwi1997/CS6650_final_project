import java.rmi.*;

public interface ChatClientInterface extends Remote {
    void receiveMessage(ChatMessage message) throws RemoteException;

    void sendMessage(String message) throws RemoteException;

    String getClientID() throws RemoteException;

    String getRoomID() throws RemoteException;

    void exitApp() throws RemoteException;
    ChatServerInterface getServer() throws RemoteException;
}