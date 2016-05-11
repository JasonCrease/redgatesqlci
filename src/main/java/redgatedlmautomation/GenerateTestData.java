package redgatedlmautomation;

import org.kohsuke.stapler.DataBoundConstructor;

public class GenerateTestData
{
    private String sqlgenPath;
    public String getSqlgenPath() {
        return sqlgenPath;
    }

    @DataBoundConstructor
    public GenerateTestData(String sqlgenPath)
    {
        this.sqlgenPath = sqlgenPath;
    }
}