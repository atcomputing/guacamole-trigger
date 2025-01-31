# Guacamole-trigger

Gucamole-trigger is a plugin for [guacamole](https://guacamole.apache.org/) that makes it possible to start and stop you remote desktop on demand.
You can write you own scripts that fits your environment.

Some [Examples](examples) are include for docker and google_cloud_platform.

This plugin also provides a way to stop remote desktops that are idle.

![screenshot](/doc/console.png)

## How it works

With this plugin you can define a `start-command` and `stop-command`.
Guacamole makes it possible for plugins to listen to the creation and closing of connections.
Based on this a start or stop command is triggered.

### automatic start

If Guacamole tries to connect to a remote desktop this plugin will check if that remote desktop is running
and will run `start-command` if that is not the case.

### automatic stop

This plugin tracks how many connections there are to each host/remote desktop.
If the last connection is closed Guacamole will run the stop command for that host.

This plugin can also track if a user is using Guacamole, and can close connection if the user has not moved the mouse or used the keyboard for `disconnect-time`
So the stop command is, run even when the user has Guacamole open in the background for a long time.

There are some limitation with automatic stop:

* This plugin only know about Guacamole connections. So it might stop machines your are still using via another method.
* This plugin only stops machines when connection count changes to 0, not when the count is already 0. So it wont stop machines that have not been connected to Guacamole
* Connection count is stored in memory. 
  - If you restart Guacamole it will lose track of which hosts are still running.
  - You cant run guacamole trigger in HA setup, were multiple guacamole instances connect to the same machines.
* This plugin will distinguish hosts by their hostname field in de connection config. If you refer to the same host with multiple names in de connection config, then Guacamole can't track the connection count correctly

If this is a problem, you can only use automatic start. Then you must configure the hosts to automatically shutdown, after no one has been logged in for a while.

## Installation
Download pluging from [release](https://github.com/atcomputing/guacamole-trigger/releases/latest)

Or compile this plug-in in with maven:
```
mvn package
```

And copy `guacamole-trigger-<version>.jar` to your [GUACAMOLE_HOME/extensions](https://guacamole.apache.org/doc/gug/configuring-guacamole.html#guacamole-home) directory.

## Usage

Configuration is done via by modifying `guacamole.properties` file.
Example Configuration can be found [here](examples)
You can set:

* `start-command`: Shell command to run for start. This line will be run by /bin/sh in UNIX systems.
    Your working directory will GUACAMOLE_HOME. All of the connection parameters of Guacamole are available
    as environment variable to this script (for example `$server_layout` and `$resize_method` for
    RDP connections). The major available parameters are

    * `$hostname` will be replaced by host you are connecting to
    * `$username` will be replaced by the username you are connecting with
    * `$guacamoleUsername` for backward compatibility with previous versions of this module
    * `$protocol` is replaced by the protocol being used by Guacamole for this connection
    * `$port` is replaced by the port used by Guacamole for this connection

For example:  `start-command: start.sh $hostname`

* `stop-command`: Shell command to run for start. This line will be run by /bin/sh in UNIX systems.
    Your working directory will GUACAMOLE_HOME. The same environment variables as the start command
    are available to the stop command

    * `$hostname` will be replaced by host you are connecting to
    * `$username` will be replaced by the username you are connecting with
    * `$guacamoleUsername` for backward compatibility with previous versions of this module
    * `$protocol` is replaced by the protocol being used by Guacamole for this connection
    * `$port` is replaced by the port used by Guacamole for this connection

  For example:  `start-command: stop.sh $hostname`.

* `shutdown-delay`: Time in seconds to wait after closing last connection before running stop command.
    This is needed for cases where disconnects are immediately followed by a try to reconnect.

    default 300

* `disconnect-time`: Time in seconds for how long a user can be idle after which the connection will be closed.
    If set to 0 connection won't be closed.

    default: 0

* `idle-time`: Time in seconds for how long a user can be idle after which user gets a warning that he will be disconnected if he stays idle.

    default: 0

* `command-timeout`: Time in seconds for how long a start/stop command can run before it's killed. To make sure every command terminates eventually.

    default: 300
    
* `console-title`: The title that will be used on the session console while the connection has not yet been established. If the variable `$hostname` is include in the title it will be replaced by the connection hostname.

  For example: `console-title: Votre machine ($hostname) est en train de démarrer"
  
  Note that the encoding of the string `console-title` must be the same as used by in the `-Dfile.encoding` variable passed in `JAVA_OPTS` and `CATALINA_OPTS`. By default this is `ISO-8859-1`. If accented or UTF-8 characters in the `console-title` are not correctly displayed, check the encoding `guacamole.properties` or modify `JAVA_OPTS` and `CATALINA_OPTS` to include the correct encoding.

    default: "Your Lab is being deployed:"

* `poll-timeout`: This in seconds to wait when polling open tunnel before calling the start script. 

    default: 5

## Writing start/stop commands

There are a couple of things you need to consider when writing start and stop commands:

* The output of your command will be displayed to the user that triggers the start command.
* exit status codes of your command are used to determine the success of the command. For now only a warning is printed in the logs of Guacamole if this is non zero.
* the time the commands take to run is the time that the plugin assumes it takes to start/stop the host.
    * If your command exits before the guacd could connect, the user gets an error message that no connection could be made.
      You could add a sleep statement to prevent this.
    * It's fine if your command is still running when guacd connects. But if it takes longer then `command-timeout` you will get a warning in the Guacamole logs
