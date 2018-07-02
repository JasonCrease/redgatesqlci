package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class GenerateTestData {
    private final String sqlgenPath;

    String getSqlgenPath() {
        return sqlgenPath;
    }

    @DataBoundConstructor
    public GenerateTestData(final String sqlgenPath) {
        this.sqlgenPath = sqlgenPath;
    }
}