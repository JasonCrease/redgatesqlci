package redgatesqlci;

import hudson.Extension;
import hudson.FilePath;
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

public class SyncBuilder extends Builder {

    private final String packageid;
    public String getPackageid() {
        return packageid;
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
    public String getFilter() { return filter; }

    private final String packageVersion;
    public String getPackageVersion() { return packageVersion;  }

    private final String isolationLevel;
    public String getIsolationLevel() { return isolationLevel; }

    private final boolean updateScript;
    public boolean getUpdateScript() { return updateScript; }

    @DataBoundConstructor
    public SyncBuilder(String packageid, String serverName, String dbName, ServerAuth serverAuth, String options, String filter, String packageVersion, String isolationLevel, boolean updateScript) {
        this.packageid = packageid;
        this.serverName = serverName;
        this.dbName = dbName;
        this.serverAuth = serverAuth.getvalue();
        this.username = serverAuth.getUsername();
        this.password = serverAuth.getPassword();
        this.options = options;
        this.filter = filter;
        this.packageVersion = packageVersion;
        this.isolationLevel = isolationLevel;
        this.updateScript = updateScript;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        ArrayList<String> params = new ArrayList<String>();

        String buildNumber = "1.0." + Integer.toString(build.getNumber());
        if(getPackageVersion() != null && !getPackageVersion().isEmpty())
            buildNumber = getPackageVersion();

        String packageFileName = Utils.constructPackageFileName(getPackageid(), buildNumber);

        params.add("Sync");
        params.add("-package");
        params.add(packageFileName);

        params.add("-databaseServer");
        params.add(getServerName());
        params.add("-databaseName");
        params.add(getDbName());

        if (getServerAuth().equals("sqlServerAuth")) {
            params.add("-databaseUserName");
            params.add(getUsername());
            params.add("-databasePassword");
            params.add(getPassword());
        }

        if (!getOptions().isEmpty()) {
            params.add("-Options");
            params.add(getOptions());
        }

        if (!getFilter().isEmpty()) {
            params.add("-filter");
            params.add(getFilter());
        }

        if (!getIsolationLevel().isEmpty()) {
            params.add("-transactionIsolationLevel");
            params.add(getIsolationLevel());
        }

        if(getUpdateScript()){
            params.add("-scriptFile");
            params.add(getPackageid() + "." + buildNumber + ".sql");
        }

        return Utils.runSQLCIWithParams(build, launcher, listener, params);
    }


    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
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
            if (packageid.length() == 0)
                return FormValidation.error("Enter a package ID");
            return FormValidation.ok();
        }

        public FormValidation doCheckDbName(@QueryParameter String dbName) throws IOException, ServletException {
            if (dbName.length() == 0)
                return FormValidation.error("Enter a database name");
            return FormValidation.ok();
        }

        public FormValidation doCheckServerName(@QueryParameter String serverName) throws IOException, ServletException {
            if (serverName.length() == 0)
                return FormValidation.error("Enter a server name");
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
            return "Redgate DLM Automation: Sync a database package";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req,formData);
        }
    }
}

