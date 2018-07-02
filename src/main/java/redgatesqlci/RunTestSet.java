package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class RunTestSet {
    private final String value;
    private final String runOnlyParams;

    public String getvalue() {
        return value;
    }

    String getRunOnlyParams() {
        return runOnlyParams;
    }

    @DataBoundConstructor
    public RunTestSet(final String value, final String runOnlyParams) {
        this.value = value;
        this.runOnlyParams = runOnlyParams;
    }
}