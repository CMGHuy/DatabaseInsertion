import org.apache.commons.cli.*;

public class BlobMain {

//    public static void main(String[] args) {
//        PushBlobDB pushBlobDB = new PushBlobDB();
//
////        String testCaseSummarizedPath = args[0];
////        String testReportZipPath = args[1];
//        String testCaseSummarizedPath = "C:\\Users\\h.cao\\Desktop\\testCaseFile.txt";
//        String testReportZipPath = "C:\\Users\\h.cao\\Desktop\\C2CTestSuiteINT-2021-04-30.zip";
//
//        pushBlobDB.insertReportResult(testCaseSummarizedPath, testReportZipPath);
//        pushBlobDB.insertFailedTests(testCaseSummarizedPath);
//    }

    public static void main(String[] args) {

        Options options = new Options();

        Option report = new Option("r", "report", true, "insert report result to mysql db" +
                "\ntwo arguments in order respectively:" +
                "\ntestCaseFile.txt path" +
                "\nzipped report result path");
        report.setArgs(2);
        options.addOption(report);

        Option progress = new Option("p", "progress", true, "update test progress on mysql db" +
                "\none argument in order respectively:" +
                "\nfolder containing all instances progress path");
        options.addOption(progress);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("r")) {
//                -r is already the args[0]
//                Example:
//                String testCaseSummarizedPath = "C:\\Users\\h.cao\\Desktop\\testCaseFile.txt";
//                String testReportZipPath = "C:\\Users\\h.cao\\Desktop\\C2CTestSuiteINT-2021-04-30.zip";
                String testCaseSummarizedPath = args[1];
                String testReportZipPath = args[2];

                PushBlobDB pushBlobDB = new PushBlobDB();
                pushBlobDB.insertReportResult(testCaseSummarizedPath, testReportZipPath);
                pushBlobDB.insertFailedTests(testCaseSummarizedPath);
            }
            if (cmd.hasOption("p")) {
//                -p is already the args[0]
//                Example:
//                String testProgressFolderPath = "C:\\Users\\h.cao\\Desktop\\currentProgress";
                String testProgressFolderPath = args[1];

                PushBlobDB pushBlobDB = new PushBlobDB();
                pushBlobDB.extractTestProgress(testProgressFolderPath);
                pushBlobDB.insertTestProgress();
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("usage:", options);

            System.exit(1);
        }
    }


}