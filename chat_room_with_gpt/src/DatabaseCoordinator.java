import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseCoordinator {
    private final DatabaseConnector[] dbConnectors;
    private final DatabaseConnector dc1;
    private final DatabaseConnector dc2;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public DatabaseCoordinator() {
        dc1 = new DatabaseConnector();
        dc2 = new DatabaseConnector();
        dc1.connectToDatabase("jdbc:mysql://localhost:3306/db1");
        dc2.connectToDatabase("jdbc:mysql://localhost:3306/db2");

        dbConnectors = new DatabaseConnector[2];
        dbConnectors[0] = dc1;
        dbConnectors[1] = dc2;
    }

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


    public boolean twoPCInsertMessage(String message, String timestamp, String clientID, String roomID) {
        List<Future<Boolean>> futuresPrepare = new ArrayList<>();
        // phase 1
        for (DatabaseConnector db : dbConnectors) {
            Future<Boolean> future = executorService.submit(() -> db.insertMessage(message, timestamp, clientID, roomID));
            futuresPrepare.add(future);
        }
        if (!checkFutures(futuresPrepare)) {
            return false;
        }
        System.out.println("Prepare phase done");
        List<Future<Boolean>> futuresCommit = new ArrayList<>();
        // phase 2
        for (DatabaseConnector db : dbConnectors) {
            Future<Boolean> future = executorService.submit(db::commitTransaction);
            futuresCommit.add(future);
        }
        if (!checkFutures(futuresCommit)) {
            System.out.println("Futures failed");
            return false;
        }
        System.out.println("Commit phase done");
        return true;
    }

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
            System.out.println("Error in checking futures: " + e.getMessage());
            return false;
        }
        return count == 2;
    }
}
