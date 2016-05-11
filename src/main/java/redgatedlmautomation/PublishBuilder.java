package redgatedlmautomation;

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

public class PublishBuilder extends Builder {

    private final String packageid;
    public String getPackageid() { return packageid; }

    private final String nugetFeedUrl;
    public String getNugetFeedUrl() { return nugetFeedUrl;  }

    private final String nugetFeedApiKey;
    public String getNugetFeedApiKey() { return nugetFeedApiKey;  }

    private final String packageVersion;
    public String getPackageVersion() { return packageVersion;  }

    @DataBoundConstructor
    public PublishBuilder(String packageid, String nugetFeedUrl, String nugetFeedApiKey, String packageVersion) {
        this.packageid = packageid;
        this.nugetFeedUrl = nugetFeedUrl;
        this.nugetFeedApiKey = nugetFeedApiKey;
        this.packageVersion = packageVersion;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        ArrayList<String> params = new ArrayList<String>();

        String buildNumber = "1.0." + Integer.toString(build.getNumber());
        if(getPackageVersion() != null && !getPackageVersion().isEmpty())
            buildNumber = getPackageVersion();

        String packageFileName = Utils.constructPackageFileName(getPackageid(), buildNumber);

        params.add("PUBLISH");
        params.add("/package=" + packageFileName);
        params.add("/nugetFeedUrl=" + getNugetFeedUrl());

        if (!getNugetFeedApiKey().isEmpty()) {
            params.add("/nugetFeedApiKey=" + getNugetFeedApiKey());
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

        public FormValidation doCheckPackageid(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Enter a package ID");
            return FormValidation.ok();
        }

        public FormValidation doCheckNugetFeedUrl(@QueryParameter String nugetFeedUrl) throws IOException, ServletException {
            if (nugetFeedUrl.length() == 0)
                return FormValidation.error("Enter a NuGet package feed URL");
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
            return "Redgate DLM Automation: Publish a database package";
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

