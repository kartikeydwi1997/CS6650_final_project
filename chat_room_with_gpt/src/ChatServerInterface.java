import java.rmi.*;
import java.util.List;

public interface ChatServerInterface extends Remote {
    public void register(ChatClientInterface client) throws RemoteException;

    void removeClient(ChatClientInterface client) throws RemoteException;

    List<ChatClientInterface> getClients() throws RemoteException;

    public void broadcast(String message, ChatClientInterface c) throws RemoteException;

    public void broadcastGPTANS(String message, ChatClientInterface c) throws RemoteException;

}