package redgatesqlci;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public class ServerAuth {
    private final String value;
    private final String username;
    private final Secret password;

    public String getvalue() {
        return value;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    @DataBoundConstructor
    public ServerAuth(final String value, final String username, final Secret password) {
        this.value = value;
        this.username = username;
        this.password = password;
    }
}
