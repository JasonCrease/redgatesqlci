package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class Server {
    private final String value;
    private final String serverName;
    private final String dbName;
    private final ServerAuth serverAuth;

    public String getvalue() {
        return value;
    }

    String getServerName() {
        return serverName;
    }

    String getDbName() {
        return dbName;
    }

    ServerAuth getServerAuth() {
        return serverAuth;
    }

    @DataBoundConstructor
    public Server(final String value, final String serverName, final String dbName, final ServerAuth serverAuth) {
        this.value = value;
        this.serverName = serverName;
        this.dbName = dbName;
        this.serverAuth = serverAuth;
    }
}