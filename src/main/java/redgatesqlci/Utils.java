package redgatesqlci;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jenkins.security.MasterToSlaveCallable;

public class Utils {    
    public static boolean runSQLCIWithParams(AbstractBuild build, Launcher launcher, BuildListener listener, ArrayList<String> params)
    {
        VirtualChannel channel = launcher.getChannel();
        
        // Check SQL CI is installed and get location.
        String sqlCiLocation = "";
        String allLocations = "";
        String[] possibleSqlCiLocations = 
            {
                getEnvironmentVariable("DLMAS_HOME", channel) +  "\\sqlci.ps1",
                getEnvironmentVariable("ProgramFiles", channel) + "\\Red Gate\\DLM Automation 2\\sqlci.ps1",
                getEnvironmentVariable("ProgramFiles(X86)", channel) + "\\Red Gate\\DLM Automation 2\\sqlci.ps1",
            };
        

        for(String possibleLocation : possibleSqlCiLocations)
        {
            if(ciExists(possibleLocation, channel)) {
                sqlCiLocation = possibleLocation;
                break;
            }
            allLocations = allLocations.concat(possibleLocation + "  ");
        }

        if(sqlCiLocation == "")
        {
            listener.error("SQLCI.ps1 cannot be found. Checked " + allLocations + ". Please install Redgate DLM Automation 2 on this agent.");
            return false;
        }

        // Set up arguments
        ArrayList<String> procParams = new ArrayList<String>();
        procParams.add(0, "\"" + getPowerShellExeLocation() + "\"");
        procParams.add(1, "-NonInteractive");
        procParams.add(2, "-ExecutionPolicy");
        procParams.add(3, "Bypass");
        procParams.add(4, "-File");
        procParams.add(5, "\"" + sqlCiLocation + "\"");
        procParams.add(6, "-Verbose");

        String longString = "\"" + getPowerShellExeLocation() + "\" -NonInteractive -ExecutionPolicy Bypass -File \"" + sqlCiLocation + "\"" + " -Verbose";

        // Here we do some parameter fiddling. Existing quotes must be escaped with three slashes
        // Then, we need to surround the part on the right of the = with quotes iff it has a space.
        for(String param : params)
        {
            // Trailing spaces can be a problem, so trim string.
            String fixedParam = param.trim();

            // Put 3 slashes before quotes (argh!!!!)
            if(fixedParam.contains("\""))
                fixedParam = fixedParam.replace("\"", "\\\\\\\"");

            // If there are spaces, surround bit after = with quotes
            if(fixedParam.contains(" "))
            {
                fixedParam = "\"" + fixedParam + "\"";
            }

            procParams.add(param);
            longString += " " + fixedParam;
        }

        // Run SQL CI with parameters. Send output and error streams to logger.Map<String, String> vars = new HashMap<String, String>();
        Map<String, String> vars = new HashMap<String, String>();
        vars.putAll(build.getBuildVariables());


        Proc proc = null;
        Launcher.ProcStarter procStarter = launcher.new ProcStarter();

        // Set process environment variables to system environment variables. This shouldn't be necessary!
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
            vars.putAll(envVars);
        }
        catch (java.io.IOException e) {}
        catch (java.lang.InterruptedException e) {}
        vars.put("REDGATE_FUR_ENVIRONMENT", "Jenkins Plugin");
        procStarter.envs(vars);

        procStarter.cmdAsSingleString(longString).stdout(listener.getLogger()).stderr(listener.getLogger()).pwd(build.getWorkspace());

        try {
            proc = launcher.launch(procStarter);
            int exitCode = proc.join();
            return exitCode == 0;
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException");
            return false;
        }
    }

    public static String constructPackageFileName(String packageName, String buildNumber)
    {
        return packageName + "." + buildNumber + ".nupkg";
    }

    private static String getEnvironmentVariable(final String variableName, VirtualChannel channel)
    {
        try {
            return channel.call(new MasterToSlaveCallable<String,RuntimeException>(){
                public String call() {
                    return System.getenv(variableName);
                }
            });
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean ciExists(final String possibleLocation, VirtualChannel channel) {
        try {
            return channel.call(new MasterToSlaveCallable<Boolean,RuntimeException>(){
                public Boolean call() {
                    return new File(possibleLocation).isFile();
                }
            });
        } catch (Exception e) {
            return false;
        }        
    }

    private static String getPowerShellExeLocation(){
        String psHome = System.getenv("PS_HOME");
        if (psHome == null) {
            psHome = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
        }
        return psHome;
    }

    public static String getEscapedOptions(String options){
        if (options.trim().startsWith("-")){
            StringBuilder sb = new StringBuilder(options);
            return sb.insert(0, ',').toString();
        }
        return options;
    }
}
