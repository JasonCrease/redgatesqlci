package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class DbFolder {
    private final String value;
    private final String subfolder;

    public String getvalue() {
        return value;
    }

    String getsubfolder() {
        return subfolder;
    }

    @DataBoundConstructor
    public DbFolder(final String value, final String subfolder) {
        this.value = value;
        this.subfolder = subfolder;
    }
}
