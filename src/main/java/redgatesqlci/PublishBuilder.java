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

@SuppressWarnings({"WeakerAccess", "InstanceVariableOfConcreteClass", "unused"})
public class PublishBuilder extends SqlContinuousIntegrationBuilder {

    private final String packageid;

    public String getPackageid() {
        return packageid;
    }

    private final String nugetFeedUrl;

    public String getNugetFeedUrl() {
        return nugetFeedUrl;
    }

    private final String nugetFeedApiKey;

    public String getNugetFeedApiKey() {
        return nugetFeedApiKey;
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
    public PublishBuilder(
        final String packageid,
        final String nugetFeedUrl,
        final String nugetFeedApiKey,
        final String packageVersion,
        final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption) {
        this.packageid = packageid;
        this.nugetFeedUrl = nugetFeedUrl;
        this.nugetFeedApiKey = nugetFeedApiKey;
        this.packageVersion = packageVersion;
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

        params.add("Publish");
        params.add("-package");
        params.add(packageFileName);
        params.add("-nugetFeedUrl");
        params.add(getNugetFeedUrl());

        if (!getNugetFeedApiKey().isEmpty()) {
            params.add("-nugetFeedApiKey");
            params.add(getNugetFeedApiKey());
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

        public FormValidation doCheckPackageid(@QueryParameter final String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Enter a package ID");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckNugetFeedUrl(
            @QueryParameter final String nugetFeedUrl) {
            if (nugetFeedUrl.isEmpty()) {
                return FormValidation.error("Enter a NuGet package feed URL");
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
            return "Redgate DLM Automation: Publish a database package";
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

