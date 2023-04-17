import java.rmi.*;
import java.util.List;

public interface ChatServerInterface extends Remote {
    public void register(ChatClientInterface client) throws RemoteException;

    void broadcast(String message, ChatClientInterface c,MessageCallback callback) throws RemoteException;

    List<ChatClientInterface> getClients() throws RemoteException;

    List<ChatMessage> sendBack() throws RemoteException;
}