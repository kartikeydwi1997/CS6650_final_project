import java.rmi.*;

public interface ChatClientInterface extends Remote {
    public void receiveMessage(ChatClientInterface c, String message) throws RemoteException;

    public String getClientID() throws RemoteException;

    public String getRoomID() throws RemoteException;
}