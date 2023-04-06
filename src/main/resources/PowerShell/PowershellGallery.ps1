$DlmAutomationModuleName = "DLMAutomation"
$SqlChangeAutomationModuleName = "SqlChangeAutomation"
$LocalModules = (New-Item "$PSScriptRoot\Modules" -ItemType Directory -Force).FullName
$env:PSModulePath = "$LocalModules;$env:PSModulePath"

function IsScaAvailable
{
    if ((Get-Module $SqlChangeAutomationModuleName) -ne $null) {
        return $true
    }

    return $false
}

function InstallCorrectSqlChangeAutomation
{
    [CmdletBinding()]
    Param(
        [Parameter(Mandatory = $false)]
        [Version]$requiredVersion
    )

    CheckRequiredDotNetVersionIsInstalled

    $moduleName = $SqlChangeAutomationModuleName

    # this will be null if $requiredVersion is not specified - which is exactly what we want
    $maximumVersion = $requiredVersion

    if ($requiredVersion) {
        if ($requiredVersion.Revision -eq -1) {
            #If provided with a 3 part version number (the 4th part, revision, == -1), we should allow any value for the revision
            $maximumVersion = [Version]"$requiredVersion.$([System.Int32]::MaxValue)"
        }

        if ($requiredVersion.Major -lt 3) {
            # If the specified version is below V3 then the user is requesting a version of DLMA. We should look for that module name instead
            $moduleName = $DlmAutomationModuleName
        }
    }

    $installedModule = GetHighestInstalledModule $moduleName -minimumVersion $requiredVersion -maximumVersion $maximumVersion

    if (!$installedModule) {
        #Either SCA isn't installed at all or $requiredVersion is specified but that version of SCA isn't installed
        Write-Verbose "$moduleName $requiredVersion not available - attempting to download from gallery"
        InstallLocalModule -moduleName $moduleName -minimumVersion $requiredVersion -maximumVersion $maximumVersion
    }
    elseif (!$requiredVersion) {
        #We've got a version of SCA installed, but $requiredVersion isn't specified so we might be able to upgrade
        $newest = GetHighestInstallableModule $moduleName
        if ($newest -and ($installedModule.Version -lt $newest.Version)) {
            Write-Verbose "Updating $moduleName to version $($newest.Version)"
            InstallLocalModule -moduleName $moduleName -minimumVersion $newest.Version
        }
    }

    # Now we're done with install/upgrade, try to import the highest available module that matches our version requirements

    # We can't just use -minimumVersion and -maximumVersion arguments on Import-Module because PowerShell 3 doesn't have them,
    # so we have to find the precise matching installed version using our code, then import that specifically. Note that
    # $requiredVersion and $maximumVersion might be null when there's no specific version we need.
    $installedModule = GetHighestInstalledModule $moduleName -minimumVersion $requiredVersion -maximumVersion $maximumVersion

    if (!$installedModule -and !$requiredVersion) {
        #Did not find SCA, and we don't have a required version so we might be able to use an installed DLMA instead.
        Write-Verbose "$moduleName is not installed - trying to fall back to $DlmAutomationModuleName"
        $installedModule = GetHighestInstalledModule $DlmAutomationModuleName        
    }
    
    if ($installedModule) {
        Write-Verbose "Importing installed $($installedModule.Name) version $($installedModule.Version)"
        Import-Module $installedModule -Force
    }
    else {
        throw "$moduleName $requiredVersion is not installed, and could not be downloaded from the PowerShell gallery"
    }
}

function CheckRequiredDotNetVersionIsInstalled {
    Write-Debug "Check .NET version pre-requisite"

    # Based on https://docs.microsoft.com/en-us/dotnet/framework/migration-guide/how-to-determine-which-versions-are-installed#ps_a
    $dotNet461Installed = Get-ChildItem "HKLM:SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full\" -ErrorAction SilentlyContinue | ? {$_.GetValue('Release') -ge 394254} 
    
    if (!$dotNet461Installed) {
        throw "SQL Change Automation requires .NET Framework 4.6.1 or later."
    }
}

function InstallPowerShellGet {
    [CmdletBinding()]
    Param()
	
    ConfigureProxyIfEnvironmentVariableSet
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12

    $psget = GetHighestInstalledModule PowerShellGet
    if (!$psget)
    {
        Write-Warning @"
Cannot access the PowerShell Gallery because PowerShellGet is not installed.
To install PowerShellGet, either upgrade to PowerShell 5 or install the PackageManagement MSI.
See https://docs.microsoft.com/en-us/powershell/gallery/installing-psget for more details.
"@
        throw "PowerShellGet is not available"
    }

    if ($psget.Version -lt [Version]'1.6') {
        #Bootstrap the NuGet package provider, which updates NuGet without requiring admin rights
        Write-Debug "Installing NuGet package provider"
        Get-PackageProvider NuGet -ForceBootstrap | Out-Null

        #Use the currently-installed version of PowerShellGet
        Import-PackageProvider PowerShellGet 
        
        #Download the version of PowerShellGet that we actually need
        Write-Debug "Installing PowershellGet"
        Save-Module -Name PowerShellGet -Path $LocalModules -MinimumVersion 1.6 -Force
    }

    Write-Debug "Importing PowershellGet"
    Import-Module PowerShellGet -MinimumVersion 1.6 -Force
    #Make sure we're actually using the package provider from the imported version of PowerShellGet
    Import-PackageProvider ((Get-Module PowerShellGet).Path) | Out-Null
}

function InstallLocalModule {
    [CmdletBinding()]
    Param(
        [Parameter(Mandatory = $true)]
        [string]$moduleName,
        [Parameter(Mandatory = $false)]
        [Version]$minimumVersion,
        [Parameter(Mandatory = $false)]
        [Version]$maximumVersion
    )
    try {
        InstallPowerShellGet

        Write-Debug "Install $moduleName $requiredVersion"
        Save-Module -Name $moduleName -Path $LocalModules -Force -AcceptLicense -MinimumVersion $minimumVersion -MaximumVersion $maximumVersion -ErrorAction Stop
    }
    catch {
        Write-Warning "Could not install $moduleName $requiredVersion from any registered PSRepository"
    }
}

function GetHighestInstalledModule {
    [CmdletBinding()]
    Param(
        [Parameter(Mandatory = $true, Position = 0)]
        [string] $moduleName,

        [Parameter(Mandatory = $false)]
        [Version]$minimumVersion,
        [Parameter(Mandatory = $false)]
        [Version]$maximumVersion
    )

    return Get-Module $moduleName -ListAvailable | 
           Where {(!$minimumVersion -or ($_.Version -ge $minimumVersion)) -and (!$maximumVersion -or ($_.Version -le $maximumVersion))} | 
           Sort -Property Version -Descending |
           Select -First 1
}

function GetHighestInstallableModule {
    [CmdletBinding()]
    Param(
        [Parameter(Mandatory = $true, Position = 0)]
        [string] $moduleName
    )

    try {
        InstallPowerShellGet
        Find-Module $moduleName -AllVersions -ErrorAction Stop | Sort -Property Version -Descending | Select -First 1    
    }
    catch {
        Write-Warning "Could not find any suitable versions of $moduleName from any registered PSRepository"
    }
}

function ConfigureProxyIfEnvironmentVariableSet {
    if (Test-Path env:REDGATE_PROXY_URL) {
        Write-Debug "Setting DefaultWebProxy to $env:REDGATE_PROXY_URL"

        [System.Net.WebRequest]::DefaultWebProxy = New-Object System.Net.WebProxy($env:REDGATE_PROXY_URL)
        [System.Net.WebRequest]::DefaultWebProxy.credentials = [System.Net.CredentialCache]::DefaultNetworkCredentials
        [System.Net.WebRequest]::DefaultWebProxy.BypassProxyOnLocal = $True
    }
}
