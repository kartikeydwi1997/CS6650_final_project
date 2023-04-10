import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
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
    private Savepoint savepoint;

    public void twoPCInsertClient(String clientID) throws SQLException {
        DatabaseConnector dc1 = new DatabaseConnector();
        DatabaseConnector dc2 = new DatabaseConnector();
        dc1.connectToDatabase("jdbc:mysql://localhost:3306/db1");
        dc2.connectToDatabase("jdbc:mysql://localhost:3306/db2");
        // Place other transaction operations here.
        try {
            // Perform SQL operation
            dc1.insertClient(clientID);
            dc2.insertClient(clientID);
            // Commit transaction if successful
            dc1.commitTransaction();
            dc2.commitTransaction();
        } catch (Exception e) {
            System.err.println("Error performing SQL operation: " + e.getMessage());
            e.printStackTrace();
            try {
                // Rollback transaction if an error occurred
                dc1.rollbackTransaction();
                dc2.rollbackTransaction();
            } catch (Exception ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public void insertClient(String clientID) throws SQLException {
        String sql = "INSERT INTO clients (client_id) VALUES (?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, clientID);
        pstmt.executeUpdate();
    }

    private void connectToDatabase(String url) {
        try {
            // String url = "jdbc:mysql://localhost:3306/ds_final";
            String user = "root";
            String password = "The1isyou";

            xaDataSource = createXADatasource(url, user, password);
            xaConnection = xaDataSource.getXAConnection();
            conn = xaConnection.getConnection();
            xaResource = xaConnection.getXAResource();

            // Begin the global transaction
            Random random = new Random();
            int formatId = random.nextInt();
            byte[] globalTransactionId = new byte[1];
            byte[] branchQualifier = new byte[1];

            random.nextBytes(globalTransactionId);
            random.nextBytes(branchQualifier);
            xid = new CustomXid(formatId, globalTransactionId, branchQualifier);
            xaResource.start(xid, XAResource.TMNOFLAGS);
            // savepoint = conn.setSavepoint("2PC_SAVEPOINT");

            System.out.println("Connected to database using 2PC protocol");

            // Perform SQL operations here and commit or rollback as needed.

        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private MysqlXADataSource createXADatasource(String url, String user, String password) {
        MysqlXADataSource dataSource = new MysqlXADataSource();
        dataSource.setUrl(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    public void commitTransaction() throws Exception {
        conn.releaseSavepoint(savepoint);
        xaResource.end(xid, XAResource.TMSUCCESS);
        int result = xaResource.prepare(xid);
        if (result == XAResource.XA_OK) {
            xaResource.commit(xid, false);
            System.out.println("Transaction committed successfully.");
        } else {
            System.out.println("Transaction commit failed.");
        }
    }

    public void rollbackTransaction() throws Exception {
        conn.rollback(savepoint);
        xaResource.end(xid, XAResource.TMFAIL);
        xaResource.rollback(xid);
        System.out.println("Transaction rolled back successfully.");
    }
}
