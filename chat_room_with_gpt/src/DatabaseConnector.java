import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import com.mysql.cj.jdbc.MysqlXADataSource;
import java.util.Random;

/**
 * A class that handles the insertion and retrieval of data from a MySQL database
 * for the chat application, using two-phase commit protocol for data consistency.
 */
public class DatabaseConnector  {
    private Connection conn;
    private XAResource xaResource;
    private Xid xid;

    /**
     * Inserts a new client with their room id into the chat database.
     * @param clientID the client that has registered
     * @param roomID the room into which the client has registered
     * @return True if the insertion was successful, false otherwise.
     */
    public boolean insertClient(String clientID, String roomID) {
        try {
            xaResource.start(xid, XAResource.TMNOFLAGS);
            String sql = "INSERT INTO clients (client_id,room_id) VALUES (?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, clientID);
            pstmt.setString(2, roomID);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException | XAException se) {
            se.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a new message into the chat database.
     * @param messageContent The message to be inserted into the database.
     * @param timestamp The timestamp of the message to be inserted into the database.
     * @param clientID The client sending the message.
     * @param roomID The room the message was sent to.
     * @return True if the insertion was successful, false otherwise.
     */
    public boolean insertMessage(String messageContent, String timestamp, String clientID, String roomID) {
        try {
            xaResource.start(xid, XAResource.TMNOFLAGS);
            String sql = "INSERT INTO messages (message_id, client_id, room_id, message_content) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, timestamp);
            pstmt.setString(2, clientID);
            pstmt.setString(3, roomID);
            pstmt.setString(4, messageContent);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException | XAException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Connect to the chat database.
     * @param url URL of the chat database to connect to
     */
    public void connectToDatabase(String url) {
        try {
            String user = "root";
            String password = "";

            XADataSource xaDataSource = createXADatasource(url, user, password);
            XAConnection xaConnection = xaDataSource.getXAConnection();
            conn = xaConnection.getConnection();
            xaResource = xaConnection.getXAResource();
            xid = createXid();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to connect to the database using the user and
     * password details
     */
    private MysqlXADataSource createXADatasource(String url, String user, String password) {
        MysqlXADataSource dataSource = new MysqlXADataSource();
        dataSource.setUrl(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * Helper method to create XID for the database transactions.
     */
    private Xid createXid() {
        byte[] gid = new byte[1];
        byte[] bid = new byte[1];
        new Random().nextBytes(gid);
        new Random().nextBytes(bid);
        return new Xid() {
            @Override
            public int getFormatId() {
                return 1;
            }

            @Override
            public byte[] getGlobalTransactionId() {
                return gid;
            }

            @Override
            public byte[] getBranchQualifier() {
                return bid;
            }
        };
    }

    /**
     * Commits the transaction to the chat database.
     * @return True if the commit was successful, false otherwise.
     */
    public boolean commitTransaction() {
        try {
            xaResource.end(xid, XAResource.TMSUCCESS);
            int prepare = xaResource.prepare(xid);

            if (prepare == XAResource.XA_OK) {
                xaResource.commit(xid, false);
                return true;
            } else {
                xaResource.rollback(xid);
                return false;
            }
        } catch (XAException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all messages from the chat database for the given parameters.
     * @return Messages from the chat database.
     */
    public ResultSet getAllMessages(String sql) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            return pstmt.executeQuery();
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }
}
