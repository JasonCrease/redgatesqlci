package redgatesqlci;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import jenkins.security.MasterToSlaveCallable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class SqlContinuousIntegrationBuilder extends Builder {
    boolean runSQLCIWithParams(
        final AbstractBuild<?, ?> build, final Launcher launcher, final TaskListener listener,
        final Iterable<String> params) {
        final VirtualChannel channel = launcher.getChannel();

        // Check SQL CI is installed and get location.
        String sqlCiLocation = "";
        final StringBuilder allLocations = new StringBuilder();
        final String[] possibleSqlCiLocations =
            new String[]{
                getEnvironmentVariable("DLMAS_HOME", channel) + "\\sqlci.ps1",
                getEnvironmentVariable("ProgramFiles", channel) + "\\Red Gate\\DLM Automation 2\\sqlci.ps1",
                getEnvironmentVariable("ProgramFiles(X86)", channel) +
                    "\\Red Gate\\DLM Automation 2\\sqlci.ps1",
            };


        for (final String possibleLocation : possibleSqlCiLocations) {
            if (ciExists(possibleLocation, channel)) {
                sqlCiLocation = possibleLocation;
                break;
            }
            allLocations.append(possibleLocation).append("  ");
        }

        if (sqlCiLocation.isEmpty()) {
            listener.error("SQLCI.ps1 cannot be found. Checked " + allLocations +
                               ". Please install Redgate DLM Automation 2 on this agent.");
            return false;
        }

        // Run SQL CI with parameters. Send output and error streams to logger.
        final ProcStarter procStarter = defineProcess(build, launcher, listener, params, sqlCiLocation);
        return executeProcess(launcher, listener, procStarter);
    }

    private ProcStarter defineProcess(
        final AbstractBuild<?, ?> build,
        final Launcher launcher,
        final TaskListener listener,
        final Iterable<String> params,
        final String sqlCiLocation) {
        final String longString = generateCmdString(params, sqlCiLocation);
        final Map<String, String> vars = getEnvironmentVariables(build, listener);
        final ProcStarter procStarter = launcher.new ProcStarter();
        procStarter.envs(vars);

        procStarter.cmdAsSingleString(longString).stdout(listener.getLogger()).stderr(listener.getLogger())
                   .pwd(build.getWorkspace());
        return procStarter;
    }

    private String generateCmdString(final Iterable<String> params, final String sqlCiLocation) {
        final StringBuilder longStringBuilder = new StringBuilder();

        longStringBuilder.append("\"").append(getPowerShellExeLocation())
                         .append("\" -NonInteractive -ExecutionPolicy Bypass -File \"").append(sqlCiLocation)
                         .append("\"").append(" -Verbose");

        // Here we do some parameter fiddling. Existing quotes must be escaped with three slashes
        // Then, we need to surround the part on the right of the = with quotes iff it has a space.
        for (final String param : params) {
            // Trailing spaces can be a problem, so trim string.
            String fixedParam = param.trim();

            // Put 3 slashes before quotes (argh!!!!)
            if (fixedParam.contains("\"")) {
                fixedParam = fixedParam.replace("\"", "\\\\\\\"");
            }

            // If there are spaces, surround bit after = with quotes
            if (fixedParam.contains(" ")) {
                fixedParam = "\"" + fixedParam + "\"";
            }

            longStringBuilder.append(" ").append(fixedParam);
        }
        return longStringBuilder.toString();
    }

    private Map<String, String> getEnvironmentVariables(final AbstractBuild<?, ?> build, final TaskListener listener) {
        final Map<String, String> vars = new HashMap<>(build.getBuildVariables());


        // Set process environment variables to system environment variables. This shouldn't be necessary!
        final EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
            vars.putAll(envVars);
        } catch (final IOException | InterruptedException ignored) {
        }
        vars.put("REDGATE_FUR_ENVIRONMENT", "Jenkins Plugin");
        return vars;
    }

    private boolean executeProcess(
        final Launcher launcher,
        final TaskListener listener,
        final ProcStarter procStarter) {
        final Proc proc;
        try {
            proc = launcher.launch(procStarter);
            final int exitCode = proc.join();
            return exitCode == 0;
        } catch (final IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException");
            return false;
        } catch (final InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException");
            return false;
        }
    }

    static String constructPackageFileName(final String packageName, final String buildNumber) {
        return packageName + "." + buildNumber + ".nupkg";
    }

    private static String getEnvironmentVariable(final String variableName, final VirtualChannel channel) {
        try {
            return channel.call(new MasterToSlaveCallable<String, RuntimeException>() {
                public String call() {
                    return System.getenv(variableName);
                }
            });
        } catch (final Exception e) {
            return null;
        }
    }

    private static boolean ciExists(final String possibleLocation, final VirtualChannel channel) {
        try {
            return channel.call(new MasterToSlaveCallable<Boolean, RuntimeException>() {
                public Boolean call() {
                    return new File(possibleLocation).isFile();
                }
            });
        } catch (final Exception e) {
            return false;
        }
    }

    private static String getPowerShellExeLocation() {
        final String psHome = System.getenv("PS_HOME");
        if (psHome != null) {
            return psHome;
        }

        return "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    }
}
