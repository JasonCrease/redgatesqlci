# Redgate DLM Automation Jenkins Plugin

## Introduction

The Redgate DLM Automation Plugin is an open-source plugin for using [Redgate DLM Automation](http://documentation.red-gate.com/display/SCI1/SQL+CI+documentation) from within Jenkins. Four tasks are available:

1. Build your database from a [Redgate SQL Source Control](http://documentation.red-gate.com/display/SOC3/SQL+Source+Control+3+documentation) source folder to a nuget package.
2. Test your database by running the tSQLt tests in the package.
3. Sync your database to another CI database.
4. Publish your database to a NuGet feed.

## Installing
If you just want to use the plugin, follow these instructions:

1. Open your Jenkins.
2. Go to Manage Jenkins > Manage Plugins> Available and search for Redgate.
3. Tick the Redgate DLM Automation Plugin, and click 'Install Without Restart'

## How to build/debug the plugin.
A basic tutorial for developing plugins is at [https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial](https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial).

Simply:

1. Clone this repository.
2. Install [Maven](https://maven.apache.org/download.cgi).
3. Open a command prompt at the repository root directory. Then run:
    * `set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n`
    * ` mvn hpi:run`
A development copy of Jenkins starts-up with the Redgate DLM Automation plugin loaded.

[JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/) is a good environment for developing and debugging Jenkins plugins. There is a free community edition.

## Help!
Email us at dlm@red-gate.com for assistance with building or using this plugin.

## "I've got an idea for an improvement"
Great! Code it up and submit a pull request. If it looks good, we'll merge it in.

Alternatively, email us at dlm@red-gate.com. We're always keen to hear your ideas and how you're using our tools.