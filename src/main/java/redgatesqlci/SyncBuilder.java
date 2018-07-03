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

import java.util.ArrayList;
import java.util.Collection;

public class SyncBuilder extends SqlContinuousIntegrationBuilder {

    private final String packageid;

    public String getPackageid() {
        return packageid;
    }

    private final String serverName;

    private String getServerName() {
        return serverName;
    }

    private final String dbName;

    private String getDbName() {
        return dbName;
    }

    private final String serverAuth;

    private String getServerAuth() {
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

    private final String packageVersion;

    public String getPackageVersion() {
        return packageVersion;
    }

    private final String isolationLevel;

    private String getIsolationLevel() {
        return isolationLevel;
    }

    private final boolean updateScript;

    private boolean getUpdateScript() {
        return updateScript;
    }

    private final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption;

    @DataBoundConstructor
    public SyncBuilder(
        final String packageid,
        final String serverName,
        final String dbName,
        final ServerAuth serverAuth,
        final String options,
        final String filter,
        final String packageVersion,
        final String isolationLevel,
        final boolean updateScript,
        final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption) {
        this.packageid = packageid;
        this.serverName = serverName;
        this.dbName = dbName;
        this.serverAuth = serverAuth.getvalue();
        username = serverAuth.getUsername();
        password = serverAuth.getPassword();
        this.options = options;
        this.filter = filter;
        this.packageVersion = packageVersion;
        this.isolationLevel = isolationLevel;
        this.updateScript = updateScript;
        this.sqlChangeAutomationVersionOption = sqlChangeAutomationVersionOption;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final Collection<String> params = new ArrayList<>();

        String buildNumber = "1.0." + Integer.toString(build.getNumber());
        if (getPackageVersion() != null && !getPackageVersion().isEmpty()) {
            buildNumber = getPackageVersion();
        }

        final String packageFileName = SqlContinuousIntegrationBuilder.constructPackageFileName(
            getPackageid(),
            buildNumber);

        params.add("Sync");
        params.add("-package");
        params.add(packageFileName);

        params.add("-databaseServer");
        params.add(getServerName());
        params.add("-databaseName");
        params.add(getDbName());

        if ("sqlServerAuth".equals(getServerAuth())) {
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

        if (getUpdateScript()) {
            params.add("-scriptFile");
            params.add(getPackageid() + "." + buildNumber + ".sql");
        }

        addProductVersionParameter(params, sqlChangeAutomationVersionOption);

        return runSqlContinuousIntegrationCmdlet(build, launcher, listener, params);
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

        public FormValidation doCheckPackageid(@QueryParameter final String packageid) {
            if (packageid.isEmpty()) {
                return FormValidation.error("Enter a package ID");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckDbName(@QueryParameter final String dbName) {
            if (dbName.isEmpty()) {
                return FormValidation.error("Enter a database name");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckServerName(
            @QueryParameter final String serverName) {
            if (serverName.isEmpty()) {
                return FormValidation.error("Enter a server name");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
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
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req, formData);
        }
    }
}

