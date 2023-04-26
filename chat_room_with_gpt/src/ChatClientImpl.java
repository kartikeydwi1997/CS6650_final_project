import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.io.Serializable;

/**
 * This class implements the ChatClientInterface, which is the remote interface
 * that clients use to send to and receive messages from the server.
 */
class ChatClientImpl extends UnicastRemoteObject implements ChatClientInterface {
    private static final String SERVER_HOST = "localhost";
    private final String clientID;
    private final String roomID;
    private final LamportClock lamportClock;
    private final ChatServerInterface server;
    private final ClientGUI gui;

    /**
     * Connect to server using RMI and create the user in the room provided.
     * @param clientID new client registering
     * @param roomID room where client is registering
     */
    public ChatClientImpl(String clientID, String roomID, int SERVER_PORT) throws IOException, NotBoundException, SQLException {
        this.clientID = clientID;
        this.roomID = roomID;
        this.lamportClock = new LamportClock();
        server = (ChatServerInterface) Naming
                .lookup("rmi://" + SERVER_HOST + ":" + SERVER_PORT + "/ChatServer");
        server.register(this);
        gui = new ClientGUI(this);
        gui.updateActiveUsersUI(server.getClients(),roomID);
    }

    /**
     * Get the client id of current user.
     * @return client id
     * @throws RemoteException thrown when remote invocation fails.
     */
    @Override
    public String getClientID() throws RemoteException {
        return this.clientID;
    }

    /**
     * Get the room id where the current client is registered
     * @return room id
     * @throws RemoteException thrown when remote invocation fails.
     */
    @Override
    public String getRoomID() throws RemoteException {
        return this.roomID;
    }

    /**
     * Sends message to the server.
     * @param message message sent by current client
     * @throws RemoteException thrown when remote invocation fails.
     */
    @Override
    public void sendMessage(String message) throws IOException {
        server.broadcast(message, this);
        if (message.contains("@BOT")) {
            server.broadcastGPTANS(message, this);
        }
    }

    /**
     * Receive messages sent by other clients in the same room
     * from the server.
     * @param message message sent by other clients
     * @throws RemoteException thrown when remote invocation fails.
     */
    @Override
    public void receiveMessage(ChatMessage message) throws RemoteException {
        lamportClock.update(message.getTimestamp());
        System.out.println("Timestamp: " + message.getTimestamp());
        System.out.println("Client " + message.getSender() + " from room ID:"+ message.getRoom() + ": "
                + message.getContent());
        gui.updateMessageUI(message);
        gui.updateActiveUsersUI(server.getClients(),message.getRoom());
    }

    /**
     * Receive ChatGPT answers replied to questions sent by other
     * clients in the same room from the server.
     * @param c client id prompting GPT
     * @param message Prompt made by the client
     * @throws RemoteException thrown when remote invocation fails.
     */
    @Override
    public void receiveAnswer(ChatClientInterface c, ChatMessage message) throws RemoteException {
        lamportClock.update(message.getTimestamp());
        System.out.println("Timestamp: " + message.getTimestamp());
        System.out.println("\u001B[31mChatGPT: \u001B[0m " + message.getContent());
        gui.updateMessageUI(message);
    }

    /**
     * Disconnects the client from the application
     * @throws RemoteException thrown when remote invocation fails.
     */
    @Override
    public void exitApp() throws RemoteException {
        server.removeClient(this);
    }
}

/**
 * This class defines the Chat Window GUI for each client when
 * they register for the app and enter a room to chat with other
 * clients in. This window has a chat area box to show chat history,
 * a text box to enter a new message and an active users list of the
 * current room.
 */
class  ClientGUI extends JFrame implements Serializable  {
    private JFrame frame;
    private final JList<String> activeUsersList;
    private final DefaultListModel<String> activeUserListModel;
    private JTextField clientTextBoard;
    private static JTextArea clientMessageBoard;
    private final ChatClientInterface client;

    /**
     * Initialzes the chat window.
     * @param client client's console
     * @throws RemoteException thrown when remote invocation fails
     * @throws SQLException thrown when SQL query in invalid
     */
    public ClientGUI(ChatClientInterface client) throws RemoteException, SQLException {
        this.client = client;
        activeUserListModel = new DefaultListModel<>();
        activeUsersList = new JList<>();
        initialize();
    }

    /**
     * Updates the chat area box with the most recent message
     * sent by any client in the room.
     * @param message message entered by an active user of room
     */
    void updateMessageUI(ChatMessage message){
        String existingText = clientMessageBoard.getText();
        String newText=(message.getSender()  + ": "
                + message.getContent());
        String text = existingText + "\n" + newText;
        clientMessageBoard.setText(text);
    }

    /**
     * Creates the GUI window using Java Swing elements
     * @throws RemoteException thrown when remote invocation fails
     */
    private void initialize() throws RemoteException {
        //Initialize the frame and set the bounds
        frame = new JFrame();
        frame.setBounds(100, 100, 888, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setTitle(client.getClientID() + "'s console");
        frame.setVisible(true);

        //Create a list to display the active users
        activeUsersList.setModel(activeUserListModel);
        activeUsersList.setToolTipText("Active Users");
        activeUsersList.setBounds(12, 420, 327, 150);
        JScrollPane scrollPaneUser = new JScrollPane(activeUsersList);
        scrollPaneUser.setBounds(12, 420, 327, 150);
        frame.getContentPane().add(scrollPaneUser);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
                    try {
                        client.exitApp();
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    System.exit(0);
                }
            }
        });

        clientMessageBoard = new JTextArea();
        clientMessageBoard.setLineWrap(true);
        clientMessageBoard.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(clientMessageBoard); // add the text area to a scroll pane
        scrollPane.setBounds(12, 51, 530, 300);
        frame.getContentPane().add(scrollPane);
        final int[] last_message_id = {0};

                try {
                    Properties props = new Properties();
                    FileInputStream fis = new FileInputStream("DBCred.properties");
                    props.load(fis);
                    String db1 = props.getProperty("db1");
                    DatabaseConnector connector = new DatabaseConnector();
                    connector.connectToDatabase(db1);
                    String sql = "SELECT message_id, client_id, message_content FROM messages WHERE room_id ='" +
                            client.getRoomID() + "' AND message_id > " + last_message_id[0] + " ORDER BY message_id;";
                    ResultSet rs = connector.getAllMessages(sql);

                    while (rs.next()) {
                        last_message_id[0] = rs.getInt("message_id");
                        String message = rs.getString("message_content");
                        String sender = rs.getString("client_id");
                        String newText =  sender + ": " + message + "\n";
                        clientMessageBoard.append(newText);
                    }

                    String existingText = clientMessageBoard.getText();
                    clientMessageBoard.setText(existingText);
                    rs.close();
                    connector.commitTransaction();
                } catch (SQLException ex) {
                    // handle exceptions here
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                } catch (FileNotFoundException e) {
                    System.out.println("File not found");
                } catch (IOException e) {
                }


        //Create label for active user list
        JLabel labelActiveUser = new JLabel("Active Users");
        labelActiveUser.setHorizontalAlignment(SwingConstants.LEFT);
        labelActiveUser.setBounds(12, 390, 95, 16);
        frame.getContentPane().add(labelActiveUser);


        //Create a text field for user to type a message to be sent to other users
        clientTextBoard = new JTextField();
        clientTextBoard.setHorizontalAlignment(SwingConstants.LEFT);
        clientTextBoard.setBounds(559, 51, 320, 200);
        frame.getContentPane().add(clientTextBoard);
        clientTextBoard.setColumns(10);

        //Create a label for the room name
        JLabel labelRoom = new JLabel("Welcome to room " + client.getRoomID());
        labelRoom.setHorizontalAlignment(SwingConstants.LEFT);
        labelRoom.setBounds(12, 25, 150, 16);
        frame.getContentPane().add(labelRoom);

        //Create a label for the clientTextBoard
        JLabel labelMessage = new JLabel("Enter Message");
        labelMessage.setHorizontalAlignment(SwingConstants.LEFT);
        labelMessage.setBounds(559, 25, 90, 16);
        frame.getContentPane().add(labelMessage);

        //Create a button for sending the message to other users
        JButton SendMessageButton = new JButton("Send");
        //Action to be taken on clicking the send button
        SendMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Read the message from text-box
                String messageTextArea = clientTextBoard.getText();
                if (messageTextArea != null && !messageTextArea.isEmpty()) {
                    try {

                        String existingText = clientMessageBoard.getText();
                        String newText=("You: "
                                + messageTextArea);
                        String text = existingText + "\n" + newText;
                        clientMessageBoard.setText(text);
                        clientTextBoard.setText("");    //Clear the text-box
                        client.sendMessage(messageTextArea);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        SendMessageButton.setBounds(559, 270, 100, 60);
        frame.getContentPane().add(SendMessageButton);
    }

    /**
     * Updates the list of active users in the room as and when
     * a new client registers
     * @param clients list of active clients
     * @param roomID room the chat window is for
     */
    public void updateActiveUsersUI(List<ChatClientInterface> clients, String roomID){
        activeUserListModel.clear();
        for (ChatClientInterface client : clients) {
            try {
                if(client.getRoomID().equals(roomID)){
                    activeUserListModel.addElement(client.getClientID());
                }
            } catch (RemoteException e) {
            }
        }
    }
}
