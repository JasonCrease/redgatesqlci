package redgatedlmautomation;

import org.kohsuke.stapler.DataBoundConstructor;

public class Server
{
    private String value;
    private String serverName;
    private String dbName;
    private ServerAuth serverAuth;

    public String getvalue() {
        return value;
    }
    public String getServerName() {
        return serverName;
    }
    public String getDbName() {
        return dbName;
    }
    public ServerAuth getServerAuth() {
        return serverAuth;
    }

    @DataBoundConstructor
    public Server(String value, String serverName, String dbName, ServerAuth serverAuth)
    {
        this.value = value;
        this.serverName = serverName;
        this.dbName = dbName;
        this.serverAuth = serverAuth;
    }
}