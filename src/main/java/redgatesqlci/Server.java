package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class Server {
    private final String value;
    private final String serverName;
    private final String dbName;
    private final ServerAuth serverAuth;
    final boolean encryptConnection;
    final boolean trustServerCertificate;

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

    public boolean getEncryptConnection() {
        return encryptConnection;
    }

    public boolean getTrustServerCertificate() {
        return trustServerCertificate;
    }

    @DataBoundConstructor
    public Server(
        final String value,
        final String serverName,
        final String dbName,
        final ServerAuth serverAuth,
        final boolean encryptConnection,
        final boolean trustServerCertificate) {
        this.value = value;
        this.serverName = serverName;
        this.dbName = dbName;
        this.serverAuth = serverAuth;
        this.encryptConnection = encryptConnection;
        this.trustServerCertificate = trustServerCertificate;
    }
}