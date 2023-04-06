package redgatesqlci;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({"InstanceVariableOfConcreteClass", "WeakerAccess", "unused"})
public class SyncBuilder extends SqlContinuousIntegrationBuilder {

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

    private Secret password;
    
    public void setPassword(Secret password) { 
        this.password = password;
    }
    
    public Secret getPassword() {
        return password;
    }

    private final boolean encryptConnection;

    public boolean getEncryptConnection() {
        return encryptConnection;
    }

    private final boolean trustServerCertificate;

    public boolean getTrustServerCertificate() {
        return trustServerCertificate;
    }

    private final String options;

    public String getOptions() {
        return options;
    }

    private final String dataOptions;

    public String getDataOptions() {
        return dataOptions;
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

    public String getIsolationLevel() {
        return isolationLevel;
    }

    private final boolean updateScript;

    public boolean getUpdateScript() {
        return updateScript;
    }

    private final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption;

    public SqlChangeAutomationVersionOption getSqlChangeAutomationVersionOption() {
        return sqlChangeAutomationVersionOption;
    }

    @DataBoundConstructor
    public SyncBuilder(
        final String packageid,
        final String serverName,
        final String dbName,
        final ServerAuth serverAuth,
        final boolean encryptConnection,
        final boolean trustServerCertificate,
        final String options,
        final String dataOptions,
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
        this.encryptConnection = encryptConnection;
        this.trustServerCertificate = trustServerCertificate;
        this.options = options;
        this.dataOptions = dataOptions;
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
        if (StringUtils.isNotEmpty(getPackageVersion())) {
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
            params.add(getPassword().getPlainText());
        }

        if (encryptConnection) {
            params.add("-encryptConnection");
        }

        if (trustServerCertificate) {
            params.add("-trustServerCertificate");
        }

        if (StringUtils.isNotEmpty(options)) {
            params.add("-Options");
            params.add(options);
        }

        if (StringUtils.isNotEmpty(dataOptions)) {
            params.add("-DataOptions");
            params.add(dataOptions);
        }

        if (StringUtils.isNotEmpty(getFilter())) {
            params.add("-filter");
            params.add(getFilter());
        }

        if (StringUtils.isNotEmpty(getIsolationLevel())) {
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
            if (StringUtils.isEmpty(packageid)) {
                return FormValidation.error("Enter a package ID");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckDbName(@QueryParameter final String dbName) {
            if (StringUtils.isEmpty(dbName)) {
                return FormValidation.error("Enter a database name");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckServerName(
            @QueryParameter final String serverName) {
            if (StringUtils.isEmpty(serverName)) {
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
            return "Redgate SQL Change Automation: Sync a database package";
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

