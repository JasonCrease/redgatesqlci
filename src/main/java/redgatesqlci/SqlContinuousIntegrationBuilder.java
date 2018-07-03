package redgatesqlci;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import redgatesqlci.SqlChangeAutomationVersionOption.ProductVersionOption;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

abstract class SqlContinuousIntegrationBuilder extends Builder {
    void addProductVersionParameter(
        final Collection<String> params,
        final SqlChangeAutomationVersionOption sqlChangeAutomationVersionOption) {
        params.add("-RequiredProductVersion");
        final String productVersion = Optional.ofNullable(sqlChangeAutomationVersionOption)
                                              .map(x -> x.getOption() == ProductVersionOption.Specific)
                                              .orElse(false)
            ? sqlChangeAutomationVersionOption.getSpecificVersion()
            : ProductVersionOption.Latest.name();
        params.add(productVersion);
    }

    boolean runSqlContinuousIntegrationCmdlet(
        final AbstractBuild<?, ?> build, final Launcher launcher, final TaskListener listener,
        final Iterable<String> params) {
        final String scaRunnerLocation;
        try {
            final URL scaRunnerUrl = getClass().getResource("/PowerShell/SqlChangeAutomationRunner.ps1");
            scaRunnerLocation = Paths.get(scaRunnerUrl.toURI()).toAbsolutePath().toString();
        } catch (final URISyntaxException e) {
            listener.error("URI syntax error: " + e.getMessage());
            return false;
        }

        // Run SQL CI with parameters. Send output and error streams to logger.
        final ProcStarter procStarter = defineProcess(build, launcher, listener, params, scaRunnerLocation);
        return executeProcess(launcher, listener, procStarter);
    }

    private ProcStarter defineProcess(
        final AbstractBuild<?, ?> build,
        final Launcher launcher,
        final TaskListener listener,
        final Iterable<String> params,
        final String scaRunnerLocation) {
        final String longString = generateCmdString(params, scaRunnerLocation);
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
            listener.error("Unexpected I/O exception executing cmdlet: " + e.getMessage());
            return false;
        } catch (final InterruptedException e) {
            listener.error("Unexpected thread interruption executing cmdlet");
            return false;
        }
    }

    static String constructPackageFileName(final String packageName, final String buildNumber) {
        return packageName + "." + buildNumber + ".nupkg";
    }

    private static String getPowerShellExeLocation() {
        final String psHome = System.getenv("PS_HOME");
        if (psHome != null) {
            return psHome;
        }

        return "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    }
}
