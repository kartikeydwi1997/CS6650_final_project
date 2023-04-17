import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import com.mysql.cj.jdbc.MysqlXADataSource;
import java.util.Random;

public class DatabaseConnector {
    private Connection conn;
    private XADataSource xaDataSource;
    private XAConnection xaConnection;
    private XAResource xaResource;
    private Xid xid;

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
        } catch (SQLException se) {
            return false;
        } catch (XAException e) {
            return false;
        }
    }

    public boolean insertMessage(String messageContent, String timestamp, String clientID, String roomID,MessageCallback callback) {
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
            callback.onSuccess(clientID,messageContent);
            return true;
        } catch (SQLException e) {
            callback.onError(e);
            return false;
        } catch (XAException e) {
            return false;
        }
    }

    public void connectToDatabase(String url) {
        try {
            String user = "root";
            String password = "";

            xaDataSource = createXADatasource(url, user, password);
            xaConnection = xaDataSource.getXAConnection();
            conn = xaConnection.getConnection();
            xaResource = xaConnection.getXAResource();
            xid = createXid();
        } catch (SQLException e) {
        }
    }

    private MysqlXADataSource createXADatasource(String url, String user, String password) {
        MysqlXADataSource dataSource = new MysqlXADataSource();
        dataSource.setUrl(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    public Xid createXid() {
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
            return false;
        }
    }

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
