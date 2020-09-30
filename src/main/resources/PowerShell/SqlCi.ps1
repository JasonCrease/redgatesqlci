<#
    .SYNOPSIS
    Allows basic usage of DLM Automation for Continuous Integration
    .DESCRIPTION
    This script mimics the old sqlci.exe behaviour using the DLM Automation cmdlets
    .PARAMETER operation
    The type of action to perform: Build, Test, TestDatabase, Sync, Publish or Activate
#>
    [CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('Build', 'BuildReadyRollProject', 'Sync', 'Test', 'TestDatabase', 'Publish', 'Activate', IgnoreCase)]
    $operation,

    [Parameter()]
    $Options,

    [Parameter()]
    $DataOptions,

    [Parameter(ValueFromRemainingArguments)]
    $operationArgs
)

# Ensure any errors are returned with an error code of 1 (failure)
trap
{
    write-output $_
    exit 1

}

# Default error action for all these functions is 'Stop'
if (!$PSBoundParameters.ContainsKey('ErrorAction'))
{
    Write-Output 'Setting ErrorActionPreference to "Stop" because no -ErrorAction argument was provided'
    $ErrorActionPreference = 'Stop'
}

$isNotImported = !(Get-Command New-DatabaseConnection -ErrorAction SilentlyContinue)

if ($isNotImported)
{
    try
    {
        Import-Module SqlChangeAutomation


    }
    catch
    {
        Import-Module DlmAutomation
    }

}
$powershellModule = Get-Module -Name SqlChangeAutomation

if ($null -eq $powershellModule)
{
    $powershellModule = Get-Module -Name DlmAutomation
}

$minimumRequiredVersionDataCompareOptions = [version]'3.3.0'
$minimumRequiredVersionTrustServerCertificate = [version]'4.3.20267'
$currentVersion = $powershellModule.Version

#region SQLCI operations

function Build
{
    <#

.SYNOPSIS
Run the equivalent of a SQL CI Build using the DLM Automation PowerShell module.

#>

    [CmdletBinding(DefaultParameterSetName = "Default")]
    param(

    # Database folder path. Path to the database scripts folder in source control. If it's a path to the build VCS root, use a period: '.'.
        [Parameter(Mandatory)]
        [string]$scriptsFolder,

    # NuGet package name. Name of the NuGet package to create. The name must not contain spaces.
        [Parameter(Mandatory)]
        [string]$packageId,

    # Build number.
    # For TeamCity, enter $(build.number)
    # For CruiseControl.NET, enter $(CCNetLabel)
    # For Bamboo, enter ${bamboo.buildNumber}
    # For TFS versions 2008 and earlier, enter $(BuildNumber)
    # For TFS versions 2010 and later, follow the instructions here: www.red-gate.com/buildnumbertfs
    # If you're using a different build system, email support@red-gate.com for help.
    # When you're using this through PowerShell, enter a number to set the build number manually.
        [Parameter(Mandatory)]
        [string]$packageVersion,

    # Path to the output folder. Ignore this if you want to default to the current working directory.
        [string]$outputFolder = ".",

    # Include database documentation in the NuGet package.
        [switch]$includeDocs,

    # DLM Dashboard hostname or IP address. Required if you want to send schema data to DLM Dashboard.
        [string]$dlmDashboardHost,

    # DLM Dashboard port number.
        [int]$dlmDashboardPort = 19528,

    # Temporary database server name. If you do not specify a server, the cmdlet will attempt to use LocalDB. Your database will be recreated on this server.
        [Parameter(ParameterSetName = 'windowsauth', Mandatory)]
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabaseServer,

    # Temporary database name. If you do not specify a database name, the cmdlet will create a scratch database. Your database will be recreated on this database
        [Parameter(ParameterSetName = 'windowsauth')]
        [Parameter(ParameterSetName = 'sqlauth')]
        [string]$temporaryDatabaseName,

    # Username for SQL authentication. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabaseUserName,

    # SQL password. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabasePassword,

    # Indicates if the database connection should be encrypted using SSL.
        [switch]$temporaryDatabaseEncryptConnection,

    # Indicates if the server certificate should be verified when connecting to a database.
        [switch]$temporaryDatabaseTrustServerCertificate,

    # DLM Automation serial number. For help finding your serial number, see https://documentation.red-gate.com/display/XX/Licensing
    # If you don't enter a key, you'll start a 28 day free trial. Separate multiple serial numbers with commas without spaces.
        [string]$licenseSerialKey,

    # SQL Compare options. You can turn off the default options or specify additional SQL Compare options.
        [string]$Options,

    # SQL Data Compare options. You can turn off the default options or specify additional SQL Data Compare options.
        [string]$DataOptions,

    # Transaction isolation level. The isolation level for the transactions during the build operation. The default level is Serializable.
        [ValidateSet('Serializable', 'Snapshot', 'RepeatableRead', 'Repeatable Read', 'ReadCommitted', 'Read Committed', 'ReadUncommitted', 'Read Uncommitted', IgnoreCase)]
        [string]$transactionIsolationLevel = 'Serializable',

    # The path to a .scpf filter file.
    # Overrides any Filter.scpf file present in the input with an alternative filter file to be used when validating and documenting the schema.
    # This parameter will be ignored if the value specified is either $null or empty.
        [string]$filter,

    # Query Batch Timeout.
    # Sets the query timeout for any sql statements being run.
    # Defaults to 30 seconds.
        [int]$queryBatchTimeout = -1,

    # The connection string to the temporary server to use. Your database will be recreated on this server
        [Parameter(ParameterSetName = 'serverconnectionstring', Mandatory)]
        [string]$temporaryServerConnectionString,

    # The connection string to the temporary database to use. Your database will be recreated on this database
        [Parameter(ParameterSetName = 'databaseconnectionstring', Mandatory)]
        [string]$temporaryDatabaseConnectionString
    )

    if ($licenseSerialKey)
    {
        Register-SqlChangeAutomation $licenseSerialKey
    }

    $temporaryConnection = BuildTemporaryConnection $temporaryDatabaseServer `
                                                    $temporaryDatabaseName `
                                                    $temporaryDatabaseUserName `
                                                    $temporaryDatabasePassword `
                                                    $temporaryServerConnectionString `
                                                    $temporaryDatabaseConnectionString `
                                                    $temporaryDatabaseEncryptConnection `
                                                    $temporaryDatabaseTrustServerCertificate

    $queryBatchTimeoutArguments = @{ }

    if ($queryBatchTimeout -ge 0)
    {
        $queryBatchTimeoutArguments += @{ 'QueryBatchTimeout' = $queryBatchTimeout }
    }

    $compareParams = CreateCompareParameters -filter $filter -compareOptions $Options -dataCompareOptions $DataOptions

    #validate the scripts folder
    $validatedSchema = $scriptsFolder | Invoke-DatabaseBuild @temporaryConnection @queryBatchTimeoutArguments @compareParams -TransactionIsolationLevel $transactionIsolationLevel

    $dlmDatabasePackageArgs = @{ 'PackageId' = $packageId; 'PackageVersion' = $packageVersion }

    if ($includeDocs)
    {

        #build the documentation
        $documentation = $validatedSchema | New-DatabaseDocumentation @temporaryConnection @compareParams

        #and add it to the arguments for the packaging process
        $dlmDatabasePackageArgs += @{ 'Documentation' = $documentation }
    }

    #now build the package
    $databasePackage = $validatedSchema | New-DatabaseBuildArtifact @dlmDatabasePackageArgs

    #save it to disk
    Export-DatabaseBuildArtifact $databasePackage -Path $outputFolder

    if ($dlmDashboardHost)
    {
        #and (optionally) tell DLM dashboard

        if ($dlmDashboardHost -inotlike 'http*')
        {
            # if it doesn't start with http, assume a scheme isn't provided (standard SQLCI args only supported hostnames)
            $uri = (New-Object -TypeName System.UriBuilder -ArgumentList "http", $dlmDashboardHost, $dlmDashboardPort).Uri
        }
        else
        {
            $uri = [System.Uri]("$( $dlmDashboardHost ):$dlmDashboardPort")
        }


        try
        {
            Publish-DatabaseBuildArtifact $databasePackage -DlmDashboardUrl $uri
            Write-Output "Published '$( $databasePackage.Name )' to DLM Dashboard at $uri"
        }
        catch
        {
            throw
        }

    }
}

function Test
{
    <#

.SYNOPSIS
Run the equivalent of a SQL CI Test using the DLM Automation PowerShell module.

#>

    [CmdletBinding(DefaultParameterSetName = "Default")]
    param
    (
    # Path to the package created in the Build step.
        [Parameter(Mandatory)]
        [string]$package,

    # Path to the output folder. Ignore this if you want to default to the current working directory.
        [string]$outputFolder = ".\",

    # Temporary database server name. If you do not specify a server, the cmdlet will attempt to use LocalDB. Your database will be recreated on this server.
        [Parameter(ParameterSetName = 'windowsauth', Mandatory)]
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabaseServer,

    # Temporary database name. If you do not specify a database name, the cmdlet will create a scratch database. Your database will be recreated on this database
        [Parameter(ParameterSetName = 'windowsauth')]
        [Parameter(ParameterSetName = 'sqlauth')]
        [string]$temporaryDatabaseName,

    # Username for SQL authentication. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabaseUserName,

    # SQL password. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabasePassword,

    # Indicates if the database connection should be encrypted using SSL.
        [switch]$temporaryDatabaseEncryptConnection,

    # Indicates if the server certificate should be verified when connecting to a database.
        [switch]$temporaryDatabaseTrustServerCertificate,

    # Populate database with test data. The path to a SQL Data Generator project file. The path must be relative to the VCS root.
        [alias("sqlDataGeneratorProject")]
        [string]$sqlDataGenerator,

    # SQL Compare options. You can turn off the default options or specify additional SQL Compare options.
        [string]$Options,

    # SQL Data Compare options. You can turn off the default options or specify additional SQL Data Compare options.
        [string]$DataOptions,

    # DLM Automation serial number. For help finding your serial number, see https://documentation.red-gate.com/display/XX/Licensing
    # If you don't enter a key, you'll start a 28 day free trial. Separate multiple serial numbers with commas without spaces.
        [string]$licenseSerialKey,

    # Test class or test to run. To run a single test, enter [testclass].[testname]. Ignore if you want to run every test class by default.
        [string]$runOnly,

    # The path to a .scpf filter file.
    # Overrides any Filter.scpf file present in the schema with an alternative filter file to be used when generating the database to test against.
    # This parameter will be ignored if the value specified is either $null or empty.
        [string]$filter,

    # Query Batch Timeout.
    # Sets the query timeout for any sql statements being run.
    # Defaults to 900 seconds.
        [int]$queryBatchTimeout = -1,

    # TestResults file name.
    # File name to use for the test result file.
    # Defaults to TestResults.
        [string]$testResultsFileName = "TestResults",

    # The connection string to the temporary server to use. Your database will be recreated on this server
        [Parameter(ParameterSetName = 'serverconnectionstring', Mandatory)]
        [string]$temporaryServerConnectionString,

    # The connection string to the temporary database to use. Your database will be recreated on this database
        [Parameter(ParameterSetName = 'databaseconnectionstring', Mandatory)]
        [string]$temporaryDatabaseConnectionString
    )

    if ($licenseSerialKey)
    {
        Register-SqlChangeAutomation $licenseSerialKey
    }

    $temporaryConnection = BuildTemporaryConnection $temporaryDatabaseServer `
                                                    $temporaryDatabaseName `
                                                    $temporaryDatabaseUserName `
                                                    $temporaryDatabasePassword `
                                                    $temporaryServerConnectionString `
                                                    $temporaryDatabaseConnectionString `
                                                    $temporaryDatabaseEncryptConnection `
                                                    $temporaryDatabaseTrustServerCertificate

    $dataGeneratorArguments = @{ }

    if ($sqlDataGenerator)
    {
        if ($sqlDataGenerator -eq $true)
        {
            $dataGeneratorArguments += @{ 'IncludeTestData' = $true }
        }
        else
        {
            $dataGeneratorArguments += @{ 'SQLDataGeneratorProject' = $sqlDataGenerator }
        }
    }

    $runOnlyArguments = @{ }

    if ($runOnly)
    {
        $runOnlyArguments += @{ 'RunOnly' = $runOnly }
    }

    $queryBatchTimeoutArguments = @{ }

    if ($queryBatchTimeout -ge 0)
    {
        $queryBatchTimeoutArguments += @{ 'QueryBatchTimeout' = $queryBatchTimeout }
    }

    $compareParams = CreateCompareParameters -filter $filter -compareOptions $Options -dataCompareOptions $DataOptions

    $testResults = $package | Invoke-DatabaseTests @temporaryConnection `
                                                      @dataGeneratorArguments `
                                                      @runOnlyArguments `
                                                      @queryBatchTimeoutArguments `
                                                      @compareParams

    # Export the test results to disk in all the supported formats
    $testResults | Export-DatabaseTestResults -OutputFile (Join-Path $outputFolder "$testResultsFileName.junit.xml") -Format JUnit -Force
    $testResults | Export-DatabaseTestResults -OutputFile (Join-Path $outputFolder "$testResultsFileName.trx") -Format MsTest -Force

    # write the test results to the output stream
    Write-Output $testResults

    if (($testResults.TotalErrors + $testResults.TotalFailures) -gt 0)
    {
        Write-Warning "FINISHED WITH ERROR: Running unit tests."
        Exit 16 #mimic sqlci.exe exit code for test failure
    }
    else
    {
        Write-Output "COMPLETED SUCCESSFULLY: Running unit tests."
    }

}


function TestDatabase
{
    <#

    .SYNOPSIS
    Runs tSQLt tests that are already present in an existing database.

    #>

    [CmdletBinding(DefaultParameterSetName = "Default")]
    param
    (
    # Path to the output folder. Ignore this if you want to default to the current working directory.
        [string]$outputFolder = ".\",

    # Temporary database server name. If you do not specify a server, the cmdlet will attempt to use LocalDB. Your database will be recreated on this server.
        [Parameter(ParameterSetName = 'windowsauth', Mandatory)]
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabaseServer,

    # Temporary database name. If you do not specify a database name, the cmdlet will create a scratch database. Your database will be recreated on this database
        [Parameter(ParameterSetName = 'windowsauth')]
        [Parameter(ParameterSetName = 'sqlauth')]
        [string]$temporaryDatabaseName,

    # Username for SQL authentication. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabaseUserName,

    # SQL password. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$temporaryDatabasePassword,

    # Indicates if the database connection should be encrypted using SSL.
        [switch]$temporaryDatabaseEncryptConnection,

    # Indicates if the server certificate should be verified when connecting to a database.
        [switch]$temporaryDatabaseTrustServerCertificate,

    # Populate database with test data. The path to a SQL Data Generator project file. The path must be relative to the VCS root.
        [alias("sqlDataGeneratorProject")]
        [string]$sqlDataGenerator,

    # SQL Compare options. You can turn off the default options or specify additional SQL Compare options.
        [string]$Options,

    # SQL Data Compare options. You can turn off the default options or specify additional SQL Data Compare options.
        [string]$DataOptions,

    # DLM Automation serial number. For help finding your serial number, see https://documentation.red-gate.com/display/XX/Licensing
    # If you don't enter a key, you'll start a 28 day free trial. Separate multiple serial numbers with commas without spaces.
        [string]$licenseSerialKey,

    # Test class or test to run. To run a single test, enter [testclass].[testname]. Ignore if you want to run every test class by default.
        [string]$runOnly,

    # The path to a .scpf filter file.
    # Overrides any Filter.scpf file present in the schema with an alternative filter file to be used when generating the database to test against.
    # This parameter will be ignored if the value specified is either $null or empty.
        [string]$filter,

    # Query Batch Timeout.
    # Sets the query timeout for any sql statements being run.
    # Defaults to 900 seconds.
        [int]$queryBatchTimeout = -1,

    # TestResults file name.
    # File name to use for the test result file.
    # Defaults to TestResults.
        [string]$testResultsFileName = "TestResults"
    )

    if ($licenseSerialKey)
    {
        Register-SqlChangeAutomation $licenseSerialKey
    }

    $connectionOptions = @{
    }
    if (AreConnectionOptionsHandled($temporaryDatabaseEncryptConnection, $temporarTestSourceOptionyDatabaseTrustServerCertificate)){
        $connectionOptions += @{ 'Encrypt' = $temporaryDatabaseEncryptConnection.ToBool() }
        $connectionOptions += @{ 'TrustServerCertificate' = $temporaryDatabaseTrustServerCertificate.ToBool() }
    }

    $databaseConnection = $null;
    if ($temporaryDatabaseUserName -And $temporaryDatabasePassword)
    {
        $databaseConnection = New-DatabaseConnection @connectionOptions `
                                                            -ServerInstance $temporaryDatabaseServer `
                                                            -Database $temporaryDatabaseName `
                                                            -Username $temporaryDatabaseUserName `
                                                            -Password $temporaryDatabasePassword
    }
    else
    {
        $databaseConnection = New-DatabaseConnection @connectionOptions `
                                                            -ServerInstance $temporaryDatabaseServer `
                                                            -Database $temporaryDatabaseName
    }

    $dataGeneratorArguments = @{ }

    if ($sqlDataGenerator)
    {
        if ($sqlDataGenerator -eq $true)
        {
            $dataGeneratorArguments += @{ 'IncludeTestData' = $true }
        }
        else
        {
            $dataGeneratorArguments += @{ 'SQLDataGeneratorProject' = $sqlDataGenerator }
        }
    }

    $runOnlyArguments = @{ }

    if ($runOnly)
    {
        $runOnlyArguments += @{ 'RunOnly' = $runOnly }
    }

    $queryBatchTimeoutArguments = @{ }

    if ($queryBatchTimeout -ge 0)
    {
        $queryBatchTimeoutArguments += @{ 'QueryBatchTimeout' = $queryBatchTimeout }
    }

    $compareParams = CreateCompareParameters -filter $filter -compareOptions $Options -dataCompareOptions $DataOptions

    $testResults = $databaseConnection | Invoke-DatabaseTests @dataGeneratorArguments `
                                                                     @runOnlyArguments `
                                                                     @queryBatchTimeoutArguments `
                                                                     @compareParams

    # Export the test results to disk in all the supported formats
    $testResults | Export-DatabaseTestResults -OutputFile (Join-Path $outputFolder "$testResultsFileName.junit.xml") -Format JUnit -Force
    $testResults | Export-DatabaseTestResults -OutputFile (Join-Path $outputFolder "$testResultsFileName.trx") -Format MsTest -Force

    # write the test results to the output stream
    Write-Output $testResults

    if (($testResults.TotalErrors + $testResults.TotalFailures) -gt 0)
    {
        Write-Warning "FINISHED WITH ERROR: Running unit tests."
        Exit 16 #mimic sqlci.exe exit code for test failure
    }
    else
    {
        Write-Output "COMPLETED SUCCESSFULLY: Running unit tests."
    }

}

function Sync
{
    <#

.SYNOPSIS
Run the equivalent of a SQL CI Sync using the DLM Automation PowerShell module.

#>

    [CmdletBinding(DefaultParameterSetName = "Default")]
    param
    (
    # Path to the package created in the Build step.
        [Parameter(Mandatory)]
        [string]$package,

    # Database server. Target database server name.
        [Parameter(Mandatory)]
        [string]$databaseServer,

    # Target database name. The target database to update with the changes in source control.
    # This must be an existing database on the server; the runner does not create the database for you.
        [Parameter(Mandatory)]
        [string]$databaseName,

    # Username for SQL authentication. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$databaseUserName,

    # SQL password. Required if you're using SQL authentication. Not required if you're using Windows authentication.
        [Parameter(ParameterSetName = 'sqlauth', Mandatory)]
        [string]$databasePassword,

    # Indicates if the database connection should be encrypted using SSL.
        [switch]$encryptConnection,

    # Indicates if the server certificate should be verified when connecting to a database.
        [switch]$trustServerCertificate,

    # DLM Automation serial number. For help finding your serial number, see https://documentation.red-gate.com/display/XX/Licensing
    # If you don't enter a key, you'll start a 28 day free trial. Separate multiple serial numbers with commas without spaces.
        [string]$licenseSerialKey,

    # SQL Compare options. You can turn off the default options or specify additional SQL Compare options.
        [string]$Options,

    # SQL Data Compare options. You can turn off the default options or specify additional SQL Data Compare options.
        [string]$DataOptions,

    # File to write the update SQL to. If you supply this optional argument, the update SQL executed to synchronize the database will be written to the specified file as well.
        [string]$scriptFile,

    # If there are deployment warnings at this level or higher, the target database will not be modified. The default value of 'None' means that warnings will be ignored.
        [ValidateSet('None', 'High', 'Medium', 'Low', 'Information')]
        [string]$abortOnWarnings = 'None',

    # Transaction isolation level. The isolation level for the transactions during the sync operation. The default level is Serializable.
        [ValidateSet('Serializable', 'Snapshot', 'RepeatableRead', 'Repeatable Read', 'ReadCommitted', 'Read Committed', 'ReadUncommitted', 'Read Uncommitted', IgnoreCase)]
        [string]$transactionIsolationLevel = 'Serializable',

    # The path to a .scpf filter file.
    # Use this parameter to specify a filter file to be used when performing the sync operation.
    # This will override any filter.scpf file in the source.
        [string]$filter,

    # Whether to ignore additional objects in the target
        [switch]$ignoreAdditional = $false,

    # Query Batch Timeout.
    # Sets the query timeout for any sql statements being run.
    # Defaults to 0 seconds.
        [int]$queryBatchTimeout = -1,

    # Target database connection string. The target database to update with the changes in source control.
    # This must be an existing database on the server; the runner does not create the database for you.
        [Parameter(ParameterSetName = 'databaseconnectionstring', Mandatory)]
        [string]$databaseConnectionString
    )

    if ($licenseSerialKey)
    {
        Register-SqlChangeAutomation $licenseSerialKey
    }

    if ($databaseConnectionString)
    {
        $targetDatabaseConnection = $databaseConnectionString
    }
    else
    {
        $connectionOptions = @{
        }
        if (AreConnectionOptionsHandled($encryptConnection, $trustServerCertificate)){
            $connectionOptions += @{ 'Encrypt' = $encryptConnection.ToBool() }
            $connectionOptions += @{ 'TrustServerCertificate' = $trustServerCertificate.ToBool() }
        }
        $targetDatabaseConnection = New-DatabaseConnection @connectionOptions -ServerInstance $databaseServer -Database $databaseName -Username $databaseUserName -Password $databasePassword
    }

    $transactionIsolationLevel = $transactionIsolationLevel -replace ' '

    $queryBatchTimeoutArguments = @{ }

    if ($queryBatchTimeout -ge 0)
    {
        $queryBatchTimeoutArguments += @{ 'QueryBatchTimeout' = $queryBatchTimeout }
    }

    $compareParams = CreateCompareParameters -filter $filter -compareOptions $Options -dataCompareOptions $DataOptions

    $syncResult = Sync-DatabaseSchema @queryBatchTimeoutArguments `
                                       @compareParams `
                                         -Source $package `
                                         -Target $targetDatabaseConnection `
                                         -AbortOnWarningLevel $abortOnWarnings `
                                         -TransactionIsolationLevel $transactionIsolationLevel `
                                         -IgnoreAdditional:$ignoreAdditional

    if ($scriptFile)
    {
        Write-Output "Writing update SQL to $scriptFile"
        $syncResult.UpdateSql | Out-File -FilePath $scriptFile
    }
}

function Publish
{
    <#

.SYNOPSIS
Run the equivalent of a SQL CI Publish using the DLM Automation PowerShell module.

#>

    [CmdletBinding(DefaultParameterSetName = "Default")]
    param
    (
    # Path to the package created in the Build step.
        [Parameter(Mandatory)]
        [string]$package,

    # NuGet feed URL. The URL of the NuGet feed to publish the package to.
        [Parameter(Mandatory)]
        [string]$nugetFeedUrl,

    # NuGet feed API key. Ignore this if you're using a public NuGet feed.
        [string]$nugetFeedApiKey,

    # DLM Automation serial number. For help finding your serial number, see https://documentation.red-gate.com/display/XX/Licensing
    # If you don't enter a key, you'll start a 28 day free trial. Separate multiple serial numbers with commas without spaces.
        [string]$licenseSerialKey
    )

    if ($licenseSerialKey)
    {
        Register-SqlChangeAutomation $licenseSerialKey
    }

    $publishArgs = @{ 'NuGetFeedUrl' = $nugetFeedUrl }
    if ($nugetFeedApiKey)
    {
        $publishArgs += @{ 'NuGetApiKey' = $nugetFeedApiKey }
    }

    Import-DatabaseBuildArtifact $package | Publish-DatabaseBuildArtifact @publishArgs
}

function Activate
{
    <#

.SYNOPSIS
Run the equivalent of a SQL CI Activate using the DLM Automation PowerShell module.

#>
    [CmdletBinding()]
    param
    (
    # DLM Automation serial number. For help finding your serial number, see https://documentation.red-gate.com/display/XX/Licensing
    # If you don't enter a key, you'll start a 28 day free trial. Separate multiple serial numbers with commas without spaces.
        [Parameter(Mandatory)]
        [string]$licenseSerialKey
    )

    Register-SqlChangeAutomation $licenseSerialKey
}

#endregion

#region Helper functions

function BuildTemporaryConnection($temporaryDatabaseServer, $temporaryDatabaseName, $temporaryDatabaseUserName, $temporaryDatabasePassword, $temporaryServerConnectionString, $temporaryDatabaseConnectionString, $temporaryDatabaseEncryptConnection, $temporaryDatabaseTrustServerCertificate)
{

    if ($temporaryServerConnectionString)
    {
        return @{ 'TemporaryDatabaseServer' = $temporaryServerConnectionString }
    }

    if ($temporaryDatabaseConnectionString)
    {
        return @{ 'TemporaryDatabase' = $temporaryDatabaseConnectionString }
    }

    if (!$temporaryDatabaseServer)
    {
        return @{ }
    }

    $connectionOptions = @{
    }
    if (AreConnectionOptionsHandled($temporaryDatabaseEncryptConnection, $temporaryDatabaseTrustServerCertificate)){
        $connectionOptions += @{ 'Encrypt' = $temporaryDatabaseEncryptConnection.ToBool() }
        $connectionOptions += @{ 'TrustServerCertificate' = $temporaryDatabaseTrustServerCertificate.ToBool() }
    }

    if ($temporaryDatabaseName)
    {
        return @{
            'TemporaryDatabase' = New-DatabaseConnection @connectionOptions `
                                                            -ServerInstance $temporaryDatabaseServer `
                                                            -Database $temporaryDatabaseName `
                                                            -Username $temporaryDatabaseUserName `
                                                            -Password $temporaryDatabasePassword
        }
    }


    return @{
        'TemporaryDatabaseServer' = New-DatabaseConnection @connectionOptions `
                                                             -ServerInstance $temporaryDatabaseServer `
                                                             -Database 'unused' `
                                                             -Username $temporaryDatabaseUserName `
                                                             -Password $temporaryDatabasePassword
    }
}

function ParseArguments()
{
    [CmdletBinding()]
    param(
        [parameter(ValueFromRemainingArguments)]$arguments
    )
    $currentName = $null
    $result = @{ }
    $arguments | ForEach {
        if ($_ -match "^-")
        {
            if ($currentName)
            {
                $result.Add($currentName, $true)
            }
            $currentName = $_.Substring(1)
        }
        else
        {
            if (-not$currentName)
            {
                throw "Cannot have value without a preceding key (-Parameter)"
            }
            $result.Add($currentName, $_)
            $currentName = $null
        }
    }

    if ($currentName)
    {
        $result.Add($currentName, $true)
    }

    return $result
}

function CreateCompareParameters($filter, $compareOptions, $dataCompareOptions)
{
    $parameters = @{
        SQLCompareOptions = $compareOptions
        FilterPath = $filter
    }

    if ([string]::IsNullOrWhiteSpace($currentVersion) -or $currentVersion -ge $minimumRequiredVersionDataCompareOptions)
    {
        $parameters.SQLDataCompareOptions = $dataCompareOptions
    }
    elseif(-not [string]::IsNullOrWhiteSpace($dataCompareOptions))
    {
        Write-Warning "SQL Data Compare options requires SQL Change Automation version $minimumRequiredVersionDataCompareOptions or later. The current version is $currentVersion."
    }

    return $parameters
}

function AreConnectionOptionsHandled($encryptConnection, $trustServerCertificate)
{
    if ([string]::IsNullOrWhiteSpace($currentVersion) -or $currentVersion -ge $minimumRequiredVersionTrustServerCertificate)
    {
        return $true
    }
    elseif($encryptConnection -or $trustServerCertificate)
    {
        Write-Warning "Encrypt and TrustServerCertificate options require SQL Change Automation version $minimumRequiredVersionTrustServerCertificate or later. The current version is $currentVersion."
        return $false
    }
}

#endregion

# split apart all the additional arguments passed to this script
$arguments = (ParseArguments @operationArgs)

if ($Options)
{
    $arguments += @{ Options = $Options }
}

if ($DataOptions)
{
    $arguments += @{ DataOptions = $DataOptions }
}

# and invoke the relevant SQLCI function with all arguments
& $operation @arguments

