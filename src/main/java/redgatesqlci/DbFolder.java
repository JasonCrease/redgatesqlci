package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class DbFolder {
    public enum ProjectOption {
        vcsroot,
        subfolder,
        scaproject
    }

    private final ProjectOption value;
    private final String subfolder;
    private final String projectPath;

    public ProjectOption getValue() {
        return value;
    }

    public String getSubfolder() {
        return subfolder;
    }

    public String getProjectPath() {
        return projectPath;
    }

    @DataBoundConstructor
    public DbFolder(final ProjectOption value, final String subfolder, final String projectPath) {
        this.value = value;
        this.subfolder = subfolder;
        this.projectPath = projectPath;
    }
}
