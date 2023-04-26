import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class creates the login window for the user to
 * register into a particular room for the chat application.
 */
public class Login extends JFrame {
    // Declare JSwing variables
    private JFrame frame;
    private JTextField clientIdTextField;
    private JTextField roomIdTextField;

    /**
     * Initializes the Login window class by creating and configuring the GUI components.
     */
//    public Login() {
//        initialize();
//    }

    /**
     * Creates the Login window class and handles the submit button click event
     * and launching the chat room window.
     */
    private void initialize(String[] args) {
        int portNumber = Integer.parseInt(args[0]);
        //Create frame and set bounds to it
        frame = new JFrame();
        frame.setBounds(100, 100, 350, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setTitle("Login");

        //Create a text-field client to enter a username
        clientIdTextField = new JTextField();
        clientIdTextField.setBounds(110, 50, 150, 50);
        frame.getContentPane().add(clientIdTextField);
        clientIdTextField.setColumns(10);

        //Create a text-field client to enter a room id
        roomIdTextField = new JTextField();
        roomIdTextField.setBounds(110, 100, 150, 50);
        frame.getContentPane().add(roomIdTextField);
        roomIdTextField.setColumns(10);

        //Create label for the client id text-field
        JLabel clientIdLabel = new JLabel("UserName:");
        clientIdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        clientIdLabel.setBounds(40, 65, 70, 16);
        frame.getContentPane().add(clientIdLabel);

        //Create label for the room id text-field
        JLabel roomIdLabel = new JLabel("Room:");
        roomIdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roomIdLabel.setBounds(45, 115, 70, 16);
        frame.getContentPane().add(roomIdLabel);

        //Create a button for connecting to the chat server
        JButton loginButton = new JButton("Connect");
        loginButton.setBounds(200, 150, 100, 50);
        frame.getContentPane().add(loginButton);

        //Action listener for clicking connect button
        loginButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            String clientId = clientIdTextField.getText();
                            String roomId = roomIdTextField.getText();

                            new ChatClientImpl(clientId, roomId,portNumber);
                            frame.dispose();
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
        );
    }

    /**
     * Launches the Login window
     * @param args none
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        //Make login window visible
                        Login window = new Login();
                        window.initialize(args);
                        window.frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );
    }
}
