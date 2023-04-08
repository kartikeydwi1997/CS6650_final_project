import java.rmi.*;

public interface ChatServerInterface extends Remote {
    public void register(ChatClientInterface client) throws RemoteException;

    public void broadcast(String message, ChatClientInterface c) throws RemoteException;
}