import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PushBlobDB {

    private Connection mySQLConn;
    private final LocalDateTime now = LocalDateTime.now();
    Map<String, TestProgress> instanceProgress = new HashMap<>();

    public PushBlobDB() {
        try {
            String sqlDriver = "com.mysql.cj.jdbc.Driver";
            Class.forName(sqlDriver);
            mySQLConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/timeline?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&maxAllowedPacket=1073741824", "root", "password");
        } catch (SQLException | ClassNotFoundException sqlException) {
            sqlException.printStackTrace();
        }
    }

    public void insertTestProgress() {
        try {
            PreparedStatement preparedStatement;
            // Remove old progress records
            String deleteOldProgress = "delete from runProgress";
            preparedStatement = mySQLConn.prepareStatement(deleteOldProgress);
            preparedStatement.executeUpdate();

            // MySQL Insert command
            String insertProgressParam = "insert into runProgress (owner, instanceIP, description, progress, passed, failed, done, remained, assigned, date) values (?,?,?,?,?,?,?,?,?,?)";
            preparedStatement = mySQLConn.prepareStatement(insertProgressParam);
            float progress;
            int passedTest;
            int failedTest;
            int doneTest;
            int remainedTest;
            int assignedTest;

            String currentTime = String.format("%d-%d-%d %d:%d:%d",
                    now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond());
            // Loop through all test progress
            for (String fileName : instanceProgress.keySet()) {
                progress = instanceProgress.get(fileName).getProgress();
                passedTest = instanceProgress.get(fileName).getPassedTest();
                failedTest = instanceProgress.get(fileName).getFailedTest();
                doneTest = instanceProgress.get(fileName).getDoneTest();
                remainedTest = instanceProgress.get(fileName).getRemainedTest();
                assignedTest = instanceProgress.get(fileName).getAssignedTest();

                preparedStatement.setString(1, "tester");
                preparedStatement.setString(2, fileName);
                preparedStatement.setString(3, fileName + " running progress");
                preparedStatement.setFloat(4, progress);
                preparedStatement.setInt(5, passedTest);
                preparedStatement.setInt(6, failedTest);
                preparedStatement.setInt(7, doneTest);
                preparedStatement.setInt(8, remainedTest);
                preparedStatement.setInt(9, assignedTest);
                preparedStatement.setString(10, currentTime);

                int count = preparedStatement.executeUpdate();
                System.out.println("isUpdated? " + count);
            }
            System.out.println("The test progress have successfully inserted into database");
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void extractTestProgress(String testProgressFolderPath) {
        File progressFolder = new File(testProgressFolderPath);
        for (File progressFile : progressFolder.listFiles()) {
            String fileName = progressFile.getName();
            String instanceIPAddress = getIPAddress(fileName);
            try (BufferedReader br = new BufferedReader(new FileReader(progressFile))) {
                String line;
                float progress = 0;
                int passed = 0, failed = 0, done = 0, remained = 0, assigned = 0;
                while ((line = br.readLine()) != null) {
                    String keywordValue = line.substring(line.lastIndexOf(":") + 1).trim();
                    if (line.contains("Progress")) {
                        progress = Float.parseFloat(keywordValue.replace("%", ""));
                    } else if (line.contains("Passed")) {
                        passed = Integer.parseInt(keywordValue);
                    } else if (line.contains("Failed")) {
                        failed = Integer.parseInt(keywordValue);
                    } else if (line.contains("Done")) {
                        done = Integer.parseInt(keywordValue);
                    } else if (line.contains("Remained")) {
                        remained = Integer.parseInt(keywordValue);
                    } else if (line.contains("Assigned")) {
                        assigned = Integer.parseInt(keywordValue);
                    }
                }
                if (fileName.contains("TestRunProgress")) {
                    instanceProgress.put(instanceIPAddress, new TestProgress(progress, passed, failed, done, remained, assigned));
                } else if (fileName.contains("currentProgress")) {
                    instanceProgress.put("currentProgress", new TestProgress(progress, passed, failed, done, remained, assigned));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getIPAddress(String fileName) {
        // Regex pattern for ip address
        Pattern pattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public void insertReportResult(String summarizedFilePath, String zipFilePath) {
        File reportResult = new File(zipFilePath);
        String failPassStatistic = readSummarizedFile(summarizedFilePath);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(reportResult))){
            // MySQL command
            String insertCommand = "insert into reportResults (owner, name, description, date, file) values (?,?,?,?,?)";
            PreparedStatement insertStatement = mySQLConn.prepareStatement(insertCommand);
            insertStatement.setString(1, "tester");
            insertStatement.setString(2, reportResult.getName());
            insertStatement.setString(3, failPassStatistic);
            insertStatement.setString(4, now.toString());
            insertStatement.setBinaryStream(5, bis, (int) reportResult.length());

            int count = insertStatement.executeUpdate();
            System.out.println("isUpdated? " + count);
            System.out.println("The selected report have successfully inserted into database");
            insertStatement.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertFailedTests(String summarizedFilePath) {
        // Example: [1]Sales_UC0030_01_Create agency account manually	FAIL	Mon Apr 26 22:15:06 2021	None
        PreparedStatement preparedStatement = null;

        try (Scanner sc = new Scanner(new File(summarizedFilePath))){
            String line;
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                String[] lineParameters = line.split("\t");

                if (lineParameters[1].contains("PASS")) {
                    preparedStatement = mySQLConn.prepareStatement("DELETE FROM failTests WHERE name=?");
                    preparedStatement.setString(1, lineParameters[0]);
                    preparedStatement.executeUpdate();
                } else if (lineParameters[1].contains("FAIL")) {
                    preparedStatement = mySQLConn.prepareStatement("DELETE FROM failTests WHERE name=?");
                    preparedStatement.setString(1, lineParameters[0]);
                    preparedStatement.executeUpdate();

                    preparedStatement = mySQLConn.prepareStatement("insert into failTests (owner, name, description, date) values (?,?,?,?)");
                    preparedStatement.setString(1, "h.cao@reply.de");
                    preparedStatement.setString(2, lineParameters[0]);
                    preparedStatement.setString(3, lineParameters[1]);
                    preparedStatement.setString(4, now.toString());
                    preparedStatement.executeUpdate();
                }
            }
        } catch (FileNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                mySQLConn.close();
                preparedStatement.close();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    public String readSummarizedFile(String summarizedFilePath) {
        // Example:
        // [1]Sales_UC0030_01_Create agency account manually	FAIL	Mon Apr 26 22:15:06 2021	None
        // 404	354	50
        try (Scanner sc = new Scanner(new File(summarizedFilePath))){
            String testResult = null;
            String lastLine = null;

            // Get the last line, containing test result params, of the file
            while (sc.hasNextLine()) {
                lastLine = sc.nextLine();
            }

            String[] lineParameters = lastLine.split("\\t");
            testResult =    "TOTAL " + lineParameters[0] + " ~~~ " +
                            "PASS " + lineParameters[1] + " ~~~ " +
                            "FAIL " + lineParameters[2];
            return testResult;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
