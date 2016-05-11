package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class ServerAuth
{
    private String value;
    private String username;
    private String password;

    public String getvalue() {
        return value;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }

    @DataBoundConstructor
    public ServerAuth(String value, String username, String password)
    {
        this.value = value;
        this.username = username;
        this.password = password;
    }
}
