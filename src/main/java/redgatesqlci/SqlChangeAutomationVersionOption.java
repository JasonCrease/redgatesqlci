package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class SqlChangeAutomationVersionOption {
    public enum ProductVersionOption {
        Latest,
        Specific
    }

    private final ProductVersionOption option;
    private final String specificVersion;

    public ProductVersionOption getOption() {
        return option;
    }

    public String getSpecificVersion() {
        return specificVersion;
    }

    @DataBoundConstructor
    public SqlChangeAutomationVersionOption(final ProductVersionOption value, final String specificVersion) {
        option = value;
        this.specificVersion = specificVersion;
    }
}
