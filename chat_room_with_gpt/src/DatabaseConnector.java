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
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public boolean twoPCInsertClient(String clientID) {
        DatabaseConnector dc1 = new DatabaseConnector();
        DatabaseConnector dc2 = new DatabaseConnector();
        dc1.connectToDatabase("jdbc:mysql://localhost:3306/db1");
        dc2.connectToDatabase("jdbc:mysql://localhost:3306/db2");

        DatabaseConnector[] dbConnectors = new DatabaseConnector[2];
        dbConnectors[0] = dc1;
        dbConnectors[1] = dc2;

        // Place other transaction operations here.
        try {
            List<Future<String>> futuresPrepare = new ArrayList<>();
            // phase 1
            for (DatabaseConnector db : dbConnectors) {
                System.out.println("In prepare phase");
                Future<String> future = executorService.submit(() -> {
                    if (db.insertClient(clientID)) {
                        return "READY";
                    } else {
                        db.rollbackTransaction();
                        return "FAIL";
                    }
                });
                futuresPrepare.add(future);
            }
            if (!checkFutures(futuresPrepare)) {
                return false;
            }
            System.out.println("Prepare phase done");

            List<Future<String>> futuresCommit = new ArrayList<>();
            // phase 2
            for (DatabaseConnector db : dbConnectors) {
                System.out.println("In commit phase");
                Future<String> future = executorService.submit(() -> {
                    if (db.commitTransaction()) {
                        return "READY";
                    } else {
                        return "FAIL";
                    }
                });
                futuresCommit.add(future);
            }
            if (!checkFutures(futuresCommit)) {
                return false;
            }
            System.out.println("Commit phase done");

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
            return false;
        }
        return true;
    }

    private boolean checkFutures(List<Future<String>> futures) {
        Boolean[] votes = {false, false};
        int index = 0;
        try {
            for (Future<String> future : futures) {
                if (future.get().equals("READY")) {
                    votes[index] = true;
                    index++;
                } else {
                    Thread.sleep(1000);
                    if (future.get().equals("READY")) {
                        votes[index] = true;
                        index++;
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return votes[0] && votes[1];
    }

    public boolean insertClient(String clientID) {
        try {
            String sql = "INSERT INTO clients (client_id) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            XAConnection xaConn = ((XADataSource)conn).getXAConnection();
            XAResource xaResource = xaConn.getXAResource();
            pstmt.setString(1, clientID);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException s) {
            System.out.println("Error in inserting client: " + clientID);
            return false;
        }

    }

    private void connectToDatabase(String url) {
        try {
            // String url = "jdbc:mysql://localhost:3306/ds_final";
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
