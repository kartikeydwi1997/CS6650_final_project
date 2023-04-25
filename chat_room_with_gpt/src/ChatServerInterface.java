import java.rmi.*;
import java.util.List;

/**
 * Defines the interface for the server used in the
 * chat application. The methods here are used for clients to
 * register for a certain room in the app, broadcast their messages
 * to all other clients as well as ask ChatGPT.
 */
public interface ChatServerInterface extends Remote {
    /**
     * Registers a new client to the database using two phase commit protocol.
     * @param client new client registering
     * @throws RemoteException thrown when remote invocation fails
     */
    void register(ChatClientInterface client) throws RemoteException;

    /**
     * Removes a client from the active clients list when they log out.
     * @param client client exiting the app
     * @throws RemoteException thrown when remote invocation fails
     */
    void removeClient(ChatClientInterface client) throws RemoteException;

    /**
     * Gets a list of all the active clients in the chat application.
     * @return list of active clients
     * @throws RemoteException thrown when remote invocation fails
     */
    List<ChatClientInterface> getClients() throws RemoteException;

    /**
     * Broadcasts a message sent by a client to a room to all the clients who
     * are registered to that room.
     * @param message message entered by the client
     * @param c client who entered the message
     * @throws RemoteException thrown when remote invocation fails
     */
    void broadcast(String message, ChatClientInterface c) throws RemoteException;

    /**
     * Broadcasts the reply from ChatGPT for the prompt sent by a client
     * to a room to all the clients who are registered to that room.
     * @param message message entered by the client
     * @param c client who entered the message
     * @throws RemoteException thrown when remote invocation fails
     */
    void broadcastGPTANS(String message, ChatClientInterface c) throws RemoteException;

}