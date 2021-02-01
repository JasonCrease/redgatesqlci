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
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import redgatesqlci.TestSource.TestSourceOption;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@SuppressWarnings({"WeakerAccess", "InstanceVariableOfConcreteClass", "unused"})
public class TestBuilder extends SqlContinuousIntegrationBuilder {
    private final TestSourceOption testSource;

    public TestSourceOption getTestSource() {
        return Optional.ofNullable(testSource).orElse(TestSourceOption.socartifact);
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

    private final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption;

    public SqlChangeAutomationVersionOption getSqlChangeAutomationVersionOption() {
        return sqlChangeAutomationVersionOption;
    }

    @DataBoundConstructor
    public TestBuilder(
        final String packageid,
        final Server tempServer,
        final RunTestSet runTestSet,
        final GenerateTestData generateTestData,
        final String options,
        final String dataOptions,
        final String filter,
        final String packageVersion,
        final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption,
        final TestSource testSource) {
        if (testSource == null) {
            this.testSource = TestSourceOption.socartifact;
            projectPath = null;
            this.packageid = packageid;
            this.packageVersion = packageVersion;
        } else {
            this.testSource = testSource.getOption();
            projectPath = testSource.getProjectPath();
            this.packageid = testSource.getPackageid();
            this.packageVersion = testSource.getProjectPath();
        }

        this.tempServer = tempServer.getvalue();
        this.runTestSet = runTestSet.getvalue();
        this.generateTestData = generateTestData == null ? null : "true";
        this.sqlChangeAutomationVersionOption = sqlChangeAutomationVersionOption;

        if ("sqlServer".equals(this.tempServer)) {
            dbName = tempServer.getDbName();
            serverName = tempServer.getServerName();
            serverAuth = tempServer.getServerAuth().getvalue();
            username = tempServer.getServerAuth().getUsername();
            password = tempServer.getServerAuth().getPassword();
            encryptConnection = tempServer.getEncryptConnection();
            trustServerCertificate = tempServer.getTrustServerCertificate();
        }
        else {
            dbName = "";
            serverName = "";
            serverAuth = null;
            username = "";
            password = Secret.fromString("");
            encryptConnection = false;
            trustServerCertificate = false;
        }

        if ("runOnlyTest".equals(this.runTestSet)) {
            runOnlyParams = runTestSet.getRunOnlyParams();
        }
        else {
            runOnlyParams = "";
        }

        if (this.generateTestData != null) {
            sqlgenPath = generateTestData.getSqlgenPath();
        }
        else {
            sqlgenPath = "";
        }

        this.options = options;
        this.dataOptions = dataOptions;
        this.filter = filter;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final Collection<String> params = new ArrayList<>();

        String buildNumber = "1.0." + Integer.toString(build.getNumber());
        if (StringUtils.isNotEmpty(getPackageVersion())) {
            buildNumber = getPackageVersion();
        }

        params.add("Test");

        final FilePath checkOutPath = build.getWorkspace();
        if (checkOutPath == null) {
            return false;
        }
        addTestSourceParameter(params, checkOutPath, buildNumber);

        if ("sqlServer".equals(getTempServer())) {
            params.add("-temporaryDatabaseServer");
            params.add(getServerName());
            if (StringUtils.isNotEmpty(getDbName())) {
                params.add("-temporaryDatabaseName");
                params.add(getDbName());
            }

            if ("sqlServerAuth".equals(getServerAuth())) {
                params.add("-temporaryDatabaseUserName");
                params.add(getUsername());
                params.add("-temporaryDatabasePassword");
                params.add(getPassword().getPlainText());
            }

            if (encryptConnection) {
                params.add("-temporaryDatabaseEncryptConnection");
            }

            if (trustServerCertificate) {
                params.add("-temporaryDatabaseTrustServerCertificate");
            }
        }

        if ("runOnlyTest".equals(getRunTestSet())) {
            params.add("-runOnly");
            params.add(getRunOnlyParams());
        }
        if (getGenerateTestData() != null) {
            params.add("-sqlDataGenerator");
            params.add(getSqlgenPath());
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

        addProductVersionParameter(params, sqlChangeAutomationVersionOption);

        return runSqlContinuousIntegrationCmdlet(build, launcher, listener, params);
    }

    private void addTestSourceParameter(final Collection<String> params, final FilePath checkOutPath, final String buildNumber) {
        params.add("-package");
        switch (getTestSource()){
            case scaproject:
                final Path projectPath = Paths.get(checkOutPath.getRemote(), this.projectPath);
                params.add(projectPath.toString());
                break;
            case socartifact:
                final String packageFileName = SqlContinuousIntegrationBuilder
                    .constructPackageFileName(getPackageid(), buildNumber);
                params.add(packageFileName);
                break;
        }
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

        public FormValidation doCheckPackageid(
            @QueryParameter final String packageid) {
            if (StringUtils.isEmpty(packageid)) {
                return FormValidation.error("Enter a package ID");
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
            return "Redgate SQL Change Automation: Test a database using tSQLt";
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

