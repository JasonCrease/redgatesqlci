[CmdletBinding()]
param(
    [Parameter(Mandatory)][String] $RequiredProductVersion,
    [Parameter(ValueFromRemainingArguments)] $arguments
)

. (Join-Path $PSScriptRoot "PowershellGallery.ps1")

try {
    if ($env:temporaryDatabasePassword -and $arguments -contains "-temporaryDatabaseUserName") {
        $arguments = $arguments + @('-temporaryDatabasePassword', $env:temporaryDatabasePassword)
    }
    if ($env:databasePassword -and $arguments -contains "-databaseUserName") {
        $arguments = $arguments + @('-databasePassword', $env:databasePassword)
    }
    
    $env:REDGATE_FUR_ENVIRONMENT = "Jenkins plugin"

    # arguments.Length is sometimes a single digit and sometimes a list of string lengths :-(
    $argslength = 0
    if ($arguments.Length.GetType() -eq $argslength.GetType()){$argslength = $arguments.Length}
    else {$argslength = $arguments.Length.Length}

    $requiredVersion = $null
    if($RequiredProductVersion -ne "Latest")
    {
        $requiredVersion = [Version]$RequiredProductVersion
    }

    InstallCorrectSqlChangeAutomation -requiredVersion $requiredVersion -Verbose

    . (Join-Path $PSScriptRoot "SqlCi.ps1") @arguments -Verbose

    if ($?){
    }
    else {
        throw "Error running SQL Change Automation action: see build log for error details"
    }
} catch {
    Write-Error $_ -ErrorAction "Continue"
    [System.Environment]::Exit(1)
}