package org.apache.guacamole.guacamoletrigger.auth;

import java.io.File;

import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

import org.apache.guacamole.GuacamoleException;

import org.slf4j.LoggerFactory;
/**
 * Utility class containing all properties used by the plugin
 * The properties defined here must be specified within
 * guacamole.properties
 */
public class ConfigurationService{

    private Environment environment;

    // for now dependency injectie is overkill
    public ConfigurationService () {
        if (environment == null){
            try {
            environment = new LocalEnvironment();
            } catch (GuacamoleException e){
                LoggerFactory.getLogger(ConfigurationService.class).error("could not read guacamole.properties. Using defaults.");
            }
        }
    }

    public static final StringGuacamoleProperty START_COMMAND =
        new StringGuacamoleProperty() {

        @Override
        public String getName() { return "start-command"; }

    };

    public static final StringGuacamoleProperty STOP_COMMAND =
        new StringGuacamoleProperty() {

        @Override
        public String getName() { return "stop-command"; }

    };

    public static final IntegerGuacamoleProperty SHUTDOWN_DELAY =
        new IntegerGuacamoleProperty () {

        @Override
        public String getName() { return "shutdown-delay"; }

    };

    public static final IntegerGuacamoleProperty IDLE_TIME =
        new IntegerGuacamoleProperty () {

        @Override
        public String getName() { return "idle-time"; }

    };

    public static final IntegerGuacamoleProperty DISCONNECT_TIME =
        new IntegerGuacamoleProperty () {

        @Override
        public String getName() { return "disconnect-time"; }

    };

    public static final IntegerGuacamoleProperty COMMAND_TIMEOUT=
        new IntegerGuacamoleProperty () {

        @Override
        public String getName() { return "command-timeout"; }

    };

    public String getStartCommand () throws GuacamoleException {
        return environment.getProperty(START_COMMAND);
    }

    public String getStopCommand () throws GuacamoleException {
        return environment.getProperty(STOP_COMMAND);
    }

    public int getShutdownDelay() throws GuacamoleException {
        return environment.getProperty(SHUTDOWN_DELAY,300);
    }

    public int getIdleTime() throws GuacamoleException {
        return environment.getProperty(IDLE_TIME,getDisconnectTime()/2);
    }

    public int getDisconnectTime() throws GuacamoleException {
        return environment.getProperty(DISCONNECT_TIME,0);
    }

    public int getCommandTimeout() throws GuacamoleException {
        return environment.getProperty(COMMAND_TIMEOUT,300);
    }

    public File getGuacamoleHome() throws GuacamoleException {
        return environment.getGuacamoleHome();
    }
}
