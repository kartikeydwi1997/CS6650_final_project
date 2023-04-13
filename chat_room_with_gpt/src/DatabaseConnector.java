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

import com.mysql.cj.jdbc.DatabaseMetaData;
import com.mysql.cj.jdbc.MysqlXADataSource;

import java.sql.Statement;
import java.util.Random;

public class DatabaseConnector {
    private Connection conn;
    private XADataSource xaDataSource;
    private XAConnection xaConnection;
    private XAResource xaResource;
    private Xid xid;
    private Savepoint savepoint;




    public boolean insertClient(String clientID,String roomID) {
        try {
            String sql = "INSERT INTO clients (client_id,room_id) VALUES (?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, clientID);
            pstmt.setString(2, roomID);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException s) {
            System.out.println("Error in inserting client: " + clientID);
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

            // Begin the global transaction
            Random random = new Random();
            int formatId = random.nextInt();
            byte[] globalTransactionId = new byte[1];
            byte[] branchQualifier = new byte[1];

            random.nextBytes(globalTransactionId);
            random.nextBytes(branchQualifier);
            xid = new CustomXid(formatId, globalTransactionId, branchQualifier);
            xaResource.start(xid, XAResource.TMNOFLAGS);

//            DatabaseMetaData metaData = (DatabaseMetaData) conn.getMetaData();
//            ResultSet tables = metaData.getTables(null, null, "clients", null);
//            if (tables.next()) {
//                System.out.println("Table exists");
//            } else {
//                xaResource.end(xid, XAResource.TMSUCCESS);
//                xaResource.commit(xid, true);
//                String sql="CREATE TABLE clients (\n" +
//                        "  client_id INT NOT NULL,\n" +
//                        "  room_id INT NOT NULL,\n" +
//                        "  PRIMARY KEY (client_id)\n" +
//                        ")";
//                PreparedStatement pstmt = conn.prepareStatement(sql);
//                pstmt.executeUpdate();
//                System.out.println("Created table in given database...");
//            }

            System.out.println("Connected to database using 2PC protocol");
            // Perform SQL operations here and commit or rollback as needed.
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
            // Roll back the transaction on error
            try {
                xaResource.rollback(xid);
            } catch (XAException xe) {
                System.err.println("Error rolling back transaction: " + xe.getMessage());
                xe.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            // Roll back the transaction on error
            try {
                xaResource.rollback(xid);
            } catch (XAException xe) {
                System.err.println("Error rolling back transaction: " + xe.getMessage());
                xe.printStackTrace();
            }

        }
    }

    private MysqlXADataSource createXADatasource(String url, String user, String password) {
        MysqlXADataSource dataSource = new MysqlXADataSource();
        dataSource.setUrl(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    public boolean commitTransaction() {
        try {
            conn.releaseSavepoint(savepoint);
            xaResource.end(xid, XAResource.TMSUCCESS);
            int result = xaResource.prepare(xid);
            if (result == XAResource.XA_OK) {
                xaResource.commit(xid, false);
                return true;
            } else {
                return false;
            }
        } catch (SQLException | XAException e) {
            return false;
        }
    }

    public void rollbackTransaction() throws Exception {
        conn.rollback(savepoint);
        xaResource.end(xid, XAResource.TMFAIL);
        xaResource.rollback(xid);
        System.out.println("Transaction rolled back successfully.");
    }
}
