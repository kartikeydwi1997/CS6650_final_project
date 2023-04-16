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
    }

    public ChatServerInterface getServer() throws RemoteException {
        return server;
    }
}

class ClientGUI extends JFrame {
    private JFrame frame;
    private JList<String> activeUsersList;
    private Set<String> activeUsers;
    private DefaultListModel<String> activeUserListModel;
    private JTextField clientTextBoard;
    private JTextArea clientMessageBoard;

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

    private void initialize() throws RemoteException, SQLException {
        // timer for polling
//        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db1", "root", "Var$So$2382");


        //Initialize the frame and set the bounds
        frame = new JFrame();
        frame.setBounds(100, 100, 888, 650);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setTitle(client.getClientID() + "'s console");
        frame.setVisible(true);

        //Create a list to display the active users
        activeUsersList.setModel(activeUserListModel);
        activeUsersList.setToolTipText("Active Users");
        activeUsersList.setBounds(12, 420, 327, 150);
        frame.getContentPane().add(activeUsersList);

        //Create text area for the clients to read messages sent to other users or sent by other users
        clientMessageBoard = new JTextArea();
        clientMessageBoard.setEditable(false);
        clientMessageBoard.setBounds(12, 51, 530, 300);
        frame.getContentPane().add(clientMessageBoard);
        final int[] last_message_id = {0};

        Timer timer = new Timer(2000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    System.out.println("Timer fired");
                    Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db1", "root", "");
                    Statement stmt = conn.createStatement();
                    String sql = "SELECT message_id, client_id, message_content FROM messages WHERE room_id =" +
                            client.getRoomID() + " AND message_id > " + last_message_id[0] + " ORDER BY message_id";
                    System.out.println("SQL: " + sql);
                    ResultSet rs = stmt.executeQuery(sql);

                    // iterate over the result set and update the GUI
                    while (rs.next()) {
                        last_message_id[0] = Integer.parseInt(rs.getString("message_id"));
                        message = rs.getString("message_content");
                        sender = rs.getString("client_id");
//                        String existingText = clientMessageBoard.getText();
//                        System.out.println("Existing text: " + existingText);
                        String newText = "\n" + sender + ": " + message;
                        System.out.println("New text: " + newText);
                        clientMessageBoard.append(newText);
                    }
                    System.out.println(message);
                    System.out.println(sender);


//                    String newText = existingText + "\n" + sender + ": " + message;
//                    System.out.println("New text: " + newText);
//                    clientMessageBoard.setText(newText);
                    rs.close();
                    stmt.close();
                    conn.close();
                } catch (SQLException ex) {
                    System.out.println("shit");
                    // handle exceptions here
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        timer.start();



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
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        SendMessageButton.setBounds(559, 270, 100, 60);
        frame.getContentPane().add(SendMessageButton);
    }
}
