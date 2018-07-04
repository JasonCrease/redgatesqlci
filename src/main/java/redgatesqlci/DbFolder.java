package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class DbFolder {
    public enum ProjectOption {
        vcsroot,
        subfolder
    }

    private final ProjectOption value;
    private final String subfolder;

    public ProjectOption getvalue() {
        return value;
    }

    String getsubfolder() {
        return subfolder;
    }

    @DataBoundConstructor
    public DbFolder(final ProjectOption value, final String subfolder) {
        this.value = value;
        this.subfolder = subfolder;
    }
}
