public class TestProgress {

    private final float progress;
    private final int passedTest;
    private final int failedTest;
    private final int doneTest;
    private final int remainedTest;
    private final int assignedTest;

    public TestProgress(float progress, int passedTest, int failedTest, int doneTest, int remainedTest, int assignedTest) {
        this.progress = progress;
        this.passedTest = passedTest;
        this.failedTest = failedTest;
        this.doneTest = doneTest;
        this.remainedTest = remainedTest;
        this.assignedTest = assignedTest;
    }

    public float getProgress() {
        return progress;
    }

    public int getPassedTest() {
        return passedTest;
    }

    public int getFailedTest() {
        return failedTest;
    }

    public int getDoneTest() {
        return doneTest;
    }

    public int getRemainedTest() {
        return remainedTest;
    }

    public int getAssignedTest() {
        return assignedTest;
    }
}
