package redgatesqlci;

import org.kohsuke.stapler.DataBoundConstructor;

public class ServerAuth {
    private final String value;
    private final String username;
    private final String password;

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
    public ServerAuth(final String value, final String username, final String password) {
        this.value = value;
        this.username = username;
        this.password = password;
    }
}
