package redgatesqlci;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;

public class TestBuilder extends SqlContinuousIntegrationBuilder {

    private final String packageid;

    public String getPackageid() {
        return packageid;
    }

    private final String tempServer;

    public String getTempServer() {
        return tempServer;
    }

    private final String serverName;

    public String getServerName() {
        return serverName;
    }

    private final String dbName;

    public String getDbName() {
        return dbName;
    }

    private final String serverAuth;

    public String getServerAuth() {
        return serverAuth;
    }

    private final String username;

    public String getUsername() {
        return username;
    }

    private final String password;

    public String getPassword() {
        return password;
    }

    private final String options;

    public String getOptions() {
        return options;
    }

    private final String filter;

    public String getFilter() {
        return filter;
    }

    private final String runOnlyParams;

    public String getRunOnlyParams() {
        return runOnlyParams;
    }

    private final String runTestSet;

    public String getRunTestSet() {
        return runTestSet;
    }

    private final String generateTestData;

    public String getGenerateTestData() {
        return generateTestData;
    }

    private final String sqlgenPath;

    public String getSqlgenPath() {
        return sqlgenPath;
    }

    private final String packageVersion;

    public String getPackageVersion() {
        return packageVersion;
    }

    @DataBoundConstructor
    public TestBuilder(
        String packageid, Server tempServer, RunTestSet runTestSet, GenerateTestData generateTestData, String options,
        String filter, String packageVersion) {

        this.packageid = packageid;
        this.tempServer = tempServer.getvalue();
        this.runTestSet = runTestSet.getvalue();
        this.generateTestData = generateTestData == null ? null : "true";

        if (this.tempServer.equals("sqlServer")) {
            this.dbName = tempServer.getDbName();
            this.serverName = tempServer.getServerName();
            this.serverAuth = tempServer.getServerAuth().getvalue();
            this.username = tempServer.getServerAuth().getUsername();
            this.password = tempServer.getServerAuth().getPassword();
        }
        else {
            this.dbName = "";
            this.serverName = "";
            this.serverAuth = "";
            this.username = "";
            this.password = "";
        }

        if (this.runTestSet.equals("runOnlyTest")) {
            this.runOnlyParams = runTestSet.getRunOnlyParams();
        }
        else {
            this.runOnlyParams = "";
        }

        if (this.generateTestData != null) {
            this.sqlgenPath = generateTestData.getSqlgenPath();
        }
        else {
            this.sqlgenPath = "";
        }

        this.options = options;
        this.filter = filter;
        this.packageVersion = packageVersion;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        ArrayList<String> params = new ArrayList<String>();

        String buildNumber = "1.0." + Integer.toString(build.getNumber());
        if (getPackageVersion() != null && !getPackageVersion().isEmpty()) {
            buildNumber = getPackageVersion();
        }

        String packageFileName = SqlContinuousIntegrationBuilder.constructPackageFileName(getPackageid(), buildNumber);

        params.add("Test");
        params.add("-package");
        params.add(packageFileName);

        if (getTempServer().equals("sqlServer")) {
            params.add("-temporaryDatabaseServer");
            params.add(getServerName());
            if (!getDbName().isEmpty()) {
                params.add("-temporaryDatabaseName");
                params.add(getDbName());
            }

            if (getServerAuth().equals("sqlServerAuth")) {
                params.add("-temporaryDatabaseUserName");
                params.add(getUsername());
                params.add("-temporaryDatabasePassword");
                params.add(getPassword());
            }
        }

        if (getRunTestSet().equals("runOnlyTest")) {
            params.add("-runOnly");
            params.add(getRunOnlyParams());
        }
        if (getGenerateTestData() != null) {
            params.add("-sqlDataGenerator");
            params.add(getSqlgenPath());
        }

        if (!getOptions().isEmpty()) {
            params.add("-Options");
            params.add(getOptions());
        }

        if (!getFilter().isEmpty()) {
            params.add("-filter");
            params.add(getFilter());
        }

        return runSQLCIWithParams(build, launcher, listener, params);
    }


    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link BuildBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckPackageid(@QueryParameter String packageid) throws IOException, ServletException {
            if (packageid.length() == 0) {
                return FormValidation.error("Enter a package ID");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Redgate DLM Automation: Test a database package";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req, formData);
        }
    }
}

