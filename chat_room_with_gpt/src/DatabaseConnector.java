import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import com.mysql.cj.jdbc.MysqlXADataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseConnector {
    private Connection conn;
    private XADataSource xaDataSource;
    private XAConnection xaConnection;
    private XAResource xaResource;
    private Xid xid;
    private Savepoint savepoint;




    public boolean insertClient(String clientID) {
        try {
            String sql = "INSERT INTO clients (client_id) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, clientID);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException s) {
            System.out.println("Error in inserting client: " + clientID);
            return false;
        }

    }

    public void connectToDatabase(String url) {
        try {
            // String url = "jdbc:mysql://localhost:3306/ds_final";
            String user = "root";
            String password = "Var$So$2382";

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
