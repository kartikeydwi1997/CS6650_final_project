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
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public DatabaseCoordinator() {
        dc1 = new DatabaseConnector();
        dc2 = new DatabaseConnector();
        dc1.connectToDatabase("jdbc:mysql://localhost:3306/db1");
        dc2.connectToDatabase("jdbc:mysql://localhost:3306/db2");

        dbConnectors = new DatabaseConnector[2];
        dbConnectors[0] = dc1;
        dbConnectors[1] = dc2;
    }

    public boolean twoPCInsertClient(String clientID,String roomID) {

        // Place other transaction operations here.
        try {
            List<Future<String>> futuresPrepare = new ArrayList<>();
            // phase 1
            for (DatabaseConnector db : dbConnectors) {
                System.out.println("In prepare phase");
                Future<String> future = executorService.submit(() -> {
                    if (db.insertClient(clientID, roomID)) {
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
                        System.out.println("commit ready");
                        return "READY";
                    } else {
                        System.out.println("commit fail");
                        return "FAIL";
                    }
                });
                futuresCommit.add(future);
            }
            if (!checkFutures(futuresCommit)) {
                System.out.println("Futures failed");
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


    public boolean twoPCInsertMessage(String message, String messageID, String clientID, String roomID) {
        try {
            List<Future<String>> futuresPrepare = new ArrayList<>();
            // phase 1
            for (DatabaseConnector db : dbConnectors) {
                Future<String> future = executorService.submit(() -> {
                    if (db.insertMessage(message, messageID, clientID, roomID)) {
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
                Future<String> future = executorService.submit(() -> {
                    if (db.commitTransaction()) {
                        System.out.println("commit ready");
                        return "READY";
                    } else {
                        System.out.println("commit fail");
                        return "FAIL";
                    }
                });
                futuresCommit.add(future);
            }
            if (!checkFutures(futuresCommit)) {
                System.out.println("Futures failed");
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
}
