import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import java.io.Serializable;
// This class implements the ChatClientInterface, which is the remote interface
// that clients use to receive messages from the server.
class ChatClientImpl extends UnicastRemoteObject implements ChatClientInterface {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;
    private final String clientID;
    private final String roomID;
    private final LamportClock lamportClock;
    private final ChatServerInterface server;
    private final ClientGUI gui;



    public ChatClientImpl(String clientID, String roomID) throws RemoteException, MalformedURLException, NotBoundException, SQLException {
        this.clientID = clientID;
        this.roomID = roomID;
        this.lamportClock = new LamportClock();
        server = (ChatServerInterface) Naming
                .lookup("rmi://" + SERVER_HOST + ":" + SERVER_PORT + "/ChatServer");
        server.register(this);
        gui = new ClientGUI(this);
        gui.updateActiveUsersUI(server.getClients(),roomID);
    }

    public String getClientID() {
        return this.clientID;
    }

    public String getRoomID() {
        return this.roomID;
    }

    public void sendMessage(String message) throws RemoteException {
        server.broadcast(message, this);
    }

    public void receiveMessage(ChatMessage message) throws RemoteException {
        lamportClock.update(message.getTimestamp());
        System.out.println("Timestamp: " + message.getTimestamp());
        System.out.println("Client " + message.getSender() + " from room ID:"+ message.getRoom() + ": "
                + message.getContent());
        gui.updateMessageUI(message);
        gui.updateActiveUsersUI(server.getClients(),message.getRoom());
    }

    public ChatServerInterface getServer() throws RemoteException {
        return server;
    }

    @Override
    public void exitApp() throws RemoteException {
        server.removeClient(this);
    }
}

class  ClientGUI extends JFrame implements Serializable  {
    private JFrame frame;
    private JList<String> activeUsersList;
    private Set<String> activeUsers;
    private DefaultListModel<String> activeUserListModel;
    private JTextField clientTextBoard;
    private static JTextArea clientMessageBoard;

    private String message;
    private String sender;

    private ChatClientInterface client;


    public ClientGUI(ChatClientInterface client) throws RemoteException, SQLException {
        this.client = client;
        activeUsers = new HashSet<>();
        activeUserListModel = new DefaultListModel<>();
        activeUsersList = new JList<>();
        initialize();
    }

    void updateMessageUI(ChatMessage message){
        String existingText = clientMessageBoard.getText();
        String newText=(message.getSender()  + ": "
                + message.getContent());
        String text = existingText + "\n" + newText;
        clientMessageBoard.setText(text);
    }

    private void initialize() throws RemoteException, SQLException {
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
        clientMessageBoard.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(clientMessageBoard); // add the text area to a scroll pane
        scrollPane.setBounds(12, 51, 530, 300);
        frame.getContentPane().add(scrollPane);
        final int[] last_message_id = {0};

                try {
                    DatabaseConnector connector = new DatabaseConnector();
                    connector.connectToDatabase("jdbc:mysql://localhost:3306/db1");
                    String sql = "SELECT message_id, client_id, message_content FROM messages WHERE room_id ='" +
                            client.getRoomID() + "' AND message_id > " + last_message_id[0] + " ORDER BY message_id;";
                    ResultSet rs = connector.getAllMessages(sql);

                    while (rs.next()) {
                        last_message_id[0] = rs.getInt("message_id");
                        message = rs.getString("message_content");
                        sender = rs.getString("client_id");
                        String newText =  sender + ": " + message+ "\n";
                        clientMessageBoard.append(newText);
                        // Do something with the message data
                    }

                    String existingText = clientMessageBoard.getText();
                    clientMessageBoard.setText(existingText);
                    rs.close();
                    connector.commitTransaction();
                } catch (SQLException ex) {
                    // handle exceptions here
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
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
                        client.sendMessage(messageTextArea);
                        String existingText = clientMessageBoard.getText();
                        String newText=("You: "
                                + messageTextArea);
                        String text = existingText + "\n" + newText;
                        clientMessageBoard.setText(text);
                        clientTextBoard.setText("");    //Clear the text-box

                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        SendMessageButton.setBounds(559, 270, 100, 60);
        frame.getContentPane().add(SendMessageButton);
    }

    public void updateActiveUsersUI(List<ChatClientInterface> clients, String roomID){
        System.out.println("Updating active users UI");
        activeUserListModel.clear();
        for (ChatClientInterface client : clients) {
            try {
                System.out.println("Adding client: " + client.getClientID());
                if(client.getRoomID().equals(roomID)){
                    activeUserListModel.addElement(client.getClientID());
                }
            } catch (RemoteException e) {
                System.out.println("User is disconnected");
            }
        }
    }
}
