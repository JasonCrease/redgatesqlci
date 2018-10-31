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
import redgatesqlci.DbFolder.ProjectOption;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({"unused", "WeakerAccess", "InstanceVariableOfConcreteClass"})
public class BuildBuilder extends SqlContinuousIntegrationBuilder {

    private final ProjectOption dbFolder;

    public ProjectOption getDbFolder() {
        return dbFolder;
    }

    private final String subfolder;

    public String getSubfolder() {
        return subfolder;
    }

    private final String projectPath;

    public String getProjectPath() {
        return projectPath;
    }

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

    private final TransactionIsolationLevel transactionIsolationLevel;

    public TransactionIsolationLevel getTransactionIsolationLevel() {
        return transactionIsolationLevel;
    }

    private final String filter;

    public String getFilter() {
        return filter;
    }

    private final String packageVersion;

    public String getPackageVersion() {
        return packageVersion;
    }

    private final boolean sendToDlmDashboard;

    public boolean getSendToDlmDashboard() {
        return sendToDlmDashboard;
    }

    private final String dlmDashboardHost;

    public String getDlmDashboardHost() {
        return dlmDashboardHost;
    }

    private final String dlmDashboardPort;

    public String getDlmDashboardPort() {
        return dlmDashboardPort;
    }

    private final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption;

    public SqlChangeAutomationVersionOption getSqlChangeAutomationVersionOption() {
        return sqlChangeAutomationVersionOption;
    }

    @DataBoundConstructor
    public BuildBuilder(
        final DbFolder dbFolder,
        final String packageid,
        final Server tempServer,
        final String options,
        final TransactionIsolationLevel transactionIsolationLevel,
        final String filter,
        final String packageVersion,
        final DlmDashboard dlmDashboard,
        final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption) {
        this.dbFolder = dbFolder.getValue();
        subfolder = dbFolder.getSubfolder();
        projectPath = dbFolder.getProjectPath();
        this.packageid = packageid;
        this.tempServer = tempServer.getvalue();
        this.sqlChangeAutomationVersionOption = sqlChangeAutomationVersionOption;

        if ("sqlServer".equals(this.tempServer)) {
            dbName = tempServer.getDbName();
            serverName = tempServer.getServerName();
            serverAuth = tempServer.getServerAuth().getvalue();
            username = tempServer.getServerAuth().getUsername();
            password = tempServer.getServerAuth().getPassword();
        }
        else {
            dbName = "";
            serverName = "";
            serverAuth = null;
            username = "";
            password = "";
        }

        this.options = options;
        this.transactionIsolationLevel = transactionIsolationLevel;
        this.filter = filter;
        this.packageVersion = packageVersion;

        sendToDlmDashboard = dlmDashboard != null;
        if (getSendToDlmDashboard()) {
            dlmDashboardHost = dlmDashboard.getDlmDashboardHost();
            dlmDashboardPort = dlmDashboard.getDlmDashboardPort();
        }
        else {
            dlmDashboardHost = null;
            dlmDashboardPort = null;
        }
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final Collection<String> params = new ArrayList<>();

        final FilePath checkOutPath = build.getWorkspace();
        if (checkOutPath == null) {
            return false;
        }
        params.add("Build");

        addProjectOptionParams(params, checkOutPath);

        params.add("-packageId");
        params.add(getPackageid());

        if (getPackageVersion() == null || getPackageVersion().isEmpty()) {
            params.add("-packageVersion");
            params.add("1.0." + build.getNumber());
        }
        else {
            params.add("-packageVersion");
            params.add(getPackageVersion());
        }

        if (!options.isEmpty()) {
            params.add("-Options");
            params.add(getEscapedOptions(getOptions()));
        }

        if (getTransactionIsolationLevel() != null) {
            params.add("-TransactionIsolationLevel");
            params.add(getTransactionIsolationLevel().name());
        }

        if (!filter.isEmpty()) {
            params.add("-filter");
            params.add(getFilter());
        }

        if ("sqlServer".equals(getTempServer())) {
            params.add("-temporaryDatabaseServer");
            params.add(getServerName());
            if (!getDbName().isEmpty()) {
                params.add("-temporaryDatabaseName");
                params.add(getDbName());
            }


            if ("sqlServerAuth".equals(getServerAuth())) {
                params.add("-temporaryDatabaseUserName");
                params.add(getUsername());
                params.add("-temporaryDatabasePassword");
                params.add(getPassword());
            }
        }

        if (getSendToDlmDashboard()) {
            params.add("-dlmDashboardHost");
            params.add(getDlmDashboardHost());
            params.add("-dlmDashboardPort");
            params.add(getDlmDashboardPort());
        }

        addProductVersionParameter(params, sqlChangeAutomationVersionOption);

        return runSqlContinuousIntegrationCmdlet(build, launcher, listener, params);
    }

    private void addProjectOptionParams(final Collection<String> params, final FilePath checkOutPath) {
        params.add("-scriptsFolder");

        switch (dbFolder){
            case scaproject:
                final Path projectPath = Paths.get(checkOutPath.getRemote(), this.projectPath);
                params.add(projectPath.toString());
                break;
            case vcsroot:
                params.add(checkOutPath.getRemote());
                break;
            case subfolder:
                final Path subfolderPath = Paths.get(checkOutPath.getRemote(), subfolder);
                params.add(subfolderPath.toString());
                break;
        }
    }

    private static String getEscapedOptions(final String options) {
        if (options.trim().startsWith("-")) {
            final StringBuilder sb = new StringBuilder(options);
            return sb.insert(0, ',').toString();
        }
        return options;
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

        public FormValidation doCheckPackageid(@QueryParameter final String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Enter a package ID.");
            }
            return FormValidation.ok();
        }

        // Since the AJAX callbacks don't give the value of radioblocks, I can't validate the value of the server and
        // database name fields.

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Redgate SQL Change Automation: Build a database package";
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

