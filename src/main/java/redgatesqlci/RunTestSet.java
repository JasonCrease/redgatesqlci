package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class RunTestSet
{
    private String value;
    private String runOnlyParams;

    public String getvalue() {
        return value;
    }
    public String getRunOnlyParams() {
        return runOnlyParams;
    }

    @DataBoundConstructor
    public RunTestSet(String value, String runOnlyParams)
    {
        this.value = value;
        this.runOnlyParams = runOnlyParams;
    }
}