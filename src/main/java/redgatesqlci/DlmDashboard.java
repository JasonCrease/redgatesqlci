package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class DlmDashboard {
    private final String dlmDashboardHost;
    private final String dlmDashboardPort;

    String getDlmDashboardHost() {
        return dlmDashboardHost;
    }

    String getDlmDashboardPort() {
        return dlmDashboardPort;
    }


    @DataBoundConstructor
    public DlmDashboard(final String dlmDashboardHost, final String dlmDashboardPort) {
        this.dlmDashboardHost = dlmDashboardHost;
        this.dlmDashboardPort = dlmDashboardPort;
    }
}
