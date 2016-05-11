package redgatedlmautomation;

import org.kohsuke.stapler.DataBoundConstructor;

public class DbFolder
{
    private String value;
    private String subfolder;

    public String getvalue() {
        return value;
    }
    public String getsubfolder() {
        return subfolder;
    }

    @DataBoundConstructor
    public DbFolder(String value, String subfolder)
    {
        this.value = value;
        this.subfolder = subfolder;
    }
}
