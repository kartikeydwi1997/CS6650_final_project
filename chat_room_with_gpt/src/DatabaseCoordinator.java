import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class acts as the coordinator for the two-phase commit protocol when
 * inserting data into database replicas.
 */
public class DatabaseCoordinator{
    private final DatabaseConnector[] dbConnectors;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * Connects to the database replicas.
     */
    public DatabaseCoordinator() throws IOException {
        DatabaseConnector dc1 = new DatabaseConnector();
        DatabaseConnector dc2 = new DatabaseConnector();
        getURLs(dc1, dc2);
        dbConnectors = new DatabaseConnector[2];
        dbConnectors[0] = dc1;
        dbConnectors[1] = dc2;
    }

    private void getURLs(DatabaseConnector dc1, DatabaseConnector dc2) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream("DBCred.properties");
        props.load(fis);
        String db1 = props.getProperty("db1");
        String db2 = props.getProperty("db2");
        dc1.connectToDatabase(db1);
        dc2.connectToDatabase(db2);
    }

    /**
     * Inserts the client logging in with the room id into each replica database
     * using the two-phase commit protocol.
     * @param clientID client logging in
     * @param roomID room client wants to enter
     * @return true or false whether the client was successfully inserted
     */
    public boolean twoPCInsertClient(String clientID, String roomID) {
        List<Future<Boolean>> futuresPrepare = new ArrayList<>();

        // phase 1
        for (DatabaseConnector db : dbConnectors) {
            Future<Boolean> future = executorService.submit(() -> db.insertClient(clientID, roomID));
            futuresPrepare.add(future);
        }

        if (!checkFutures(futuresPrepare)) {
            return false;
        }

        List<Future<Boolean>> futuresCommit = new ArrayList<>();
        // phase 2
        for (DatabaseConnector db : dbConnectors) {
            Future<Boolean> future = executorService.submit(() -> db.commitTransaction());
            futuresCommit.add(future);
        }
        if (!checkFutures(futuresCommit)) {
            return false;
        }
        return true;
    }

    /**
     * Inserts the message sent by each client in the chat room
     * with the room id into each replica database using the two-phase commit protocol.
     * @param message message entered by client
     * @param timestamp lamport timestamp for each message
     * @param clientID client sending the message
     * @param roomID room the client sent message
     * @return true or false whether the message was successfully inserted
     */
    public boolean twoPCInsertMessage(String message, String timestamp, String clientID, String roomID) {
        List<Future<Boolean>> futuresPrepare = new ArrayList<>();
        // phase 1
        for (DatabaseConnector db : dbConnectors) {
            Future<Boolean> future = executorService
                    .submit(() -> db.insertMessage(message, timestamp, clientID, roomID));
            futuresPrepare.add(future);
        }
        if (!checkFutures(futuresPrepare)) {
            return false;
        }
        List<Future<Boolean>> futuresCommit = new ArrayList<>();
        // phase 2
        for (DatabaseConnector db : dbConnectors) {
            Future<Boolean> future = executorService.submit(db::commitTransaction);
            futuresCommit.add(future);
        }
        if (!checkFutures(futuresCommit)) {
            return false;
        }
        return true;
    }

    /**
     * Helper method to check if all the replicas agreed to commit.
     */
    private boolean checkFutures(List<Future<Boolean>> futures) {
        int count = 0;
        try {
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    count++;
                } else {
                    Thread.sleep(1000);
                    if (future.get()) {
                        count++;
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
        return count == 2;
    }
}
