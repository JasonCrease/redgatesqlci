package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class DlmDashboard {
    private String dlmDashboardHost;
    private String dlmDashboardPort;

    public String getDlmDashboardHost() {
        return dlmDashboardHost;
    }

    public String getDlmDashboardPort() {
        return dlmDashboardPort;
    }


    @DataBoundConstructor
    public DlmDashboard(String dlmDashboardHost, String dlmDashboardPort) {
        this.dlmDashboardHost = dlmDashboardHost;
        this.dlmDashboardPort = dlmDashboardPort;
    }
}
