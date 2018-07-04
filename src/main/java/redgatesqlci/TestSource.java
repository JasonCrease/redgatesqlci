package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings({"unused", "WeakerAccess"})
public class TestSource {
    public enum TestSourceOption {
        scaproject,
        socartifact
    }

    private final TestSourceOption option;
    private final String projectPath;
    private final String packageid;
    private final String packageVersion;

    @DataBoundConstructor
    public TestSource(
        final TestSourceOption value,
        final String projectPath,
        final String packageid,
        final String packageVersion) {
        option = value;
        this.projectPath = projectPath;
        this.packageid = packageid;
        this.packageVersion = packageVersion;
    }

    public TestSourceOption getOption() {
        return option;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getPackageid() {
        return packageid;
    }

    public String getPackageVersion() {
        return packageVersion;
    }
}
