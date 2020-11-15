# Guacamole-trigger

Gucamole-trigger is a plugin for [guacamole](https://guacamole.apache.org/) that makes it possible to start and stop you remote desktop on demand.
You can wright you own scripts that fits your environment.

Some [Examples](examples) are include for docker and google_cloud_platform.

This plugin also provides a way to stop remote desktops that are idle.

## How it works

With this plugin you can define a `start-command` and `stop-command`.
Guacamole makes it possible for plugins to listen to the creation and closing of connections.
Based on this start/stop command are trigged.

### automatic start

If Guacamole try's to connection to a remote desktop. This plugin will check if that remote desktop is running.
and will run `start-command` if that is not the case.

### automatic stop

This plugin tracks how many connection there are to each host/remote desktop.
If the last connection is closed Guacamole will run the stop command for that host.

This plugin also can track if a user is using Guacamole, and can close connection if the user has not moved it's mouse ore used its keyboard for `disconnect-time`
So the stop command is, run even when the user has Guacamole open in the background for a long time.

there are some limitation:

* This plugin only know about Guacamole connections. So it might stop machines your are still using via another method.
* This plugin only stop machines when connection count reaches 0. not that are already 0. So it wont stop machines that have not been connected to Guacamole
* if you restart Guacamole it will lose track of which host are still running
* This plugin will distinguish host by there hostname field in de connection config. if you refer to the same host with multiple names in de connection config. then t Guacamole can't track the connection count correctly

## Installation

Compile this plug-in in with maven:
```
mvn package
```

And copy target/guacamole-trigger-<version>.jar to your [GUACAMOLE_HOME/extensions](https://guacamole.apache.org/doc/gug/configuring-guacamole.html#guacamole-home) directory.

## Usage

Configuration is done via by modifying `guacamole.properties` file.
example Configuration can be found [here](examples)
you can set:

* `start-command`: Shell command to run for start. This line will be run by /bin/sh in UNIX systems.
    Your working directory will GUACAMOLE_HOME.

    * `$hostname` will be replaced by host you are connection to
    * `guacamoleUsername` is replaced by which Guacamole user is trying to connect

  For example:  `start-command: start.sh $hostname`

* `stop-command`: Shell command to run for start. This line will be run by /bin/sh in UNIX systems.
    Your working directory will GUACAMOLE_HOME.

    * `$hostname` will be replaced by host you are connection to
    * `guacamoleUsername` is replaced by which Guacamole user is trying to connect

  For example:  `start-command: stop.sh $hostname`.

* `shutdown-delay`: Time in seconds to wait after closing last connection before running stop command.
    This is need for case where disconnect and immediately try to reconnect.

    default 300

* `disconnect-time`: Time in seconds for how long a user can be idle after which the connection will be closed.
    If set to 0 connection won't be closed.

    default: 0

* `idle-time`: Time in seconds for how long a user can be idle after which user gets a warning that he will be disconnected if he stays idle.

    default: 0

* `command-timeout`: Time in seconds for how long a start/stop command can run before it's killed. To make sure every command terminates eventually.

    default: 300

## Writing start/stop commands

There are a couple of think you need to consider when writing start and stop commands:

* The output of you command will be displayed to the user that triggers the start command.
* exit status codes of your command are used to determine the succes of the command. For now only a warning is printed in the logs of Guacamole of this non zero.
* the time the commands take to run is the time that the plugin assumes it takes to start/stop the host.
    * If your command exits before the guacd could connect. the user gets a error messages that no connection could be made.
      You could add a sleep statement to prevent this.
    * It's fine if your command is still running when guacd connects. But if it takes longer then `command-timeout` you will get a warning in the Guacamole logs
