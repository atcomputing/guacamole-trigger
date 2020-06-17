package org.apache.guacamole.guacamoletrigger.auth;

import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
/**
 * Utility class containing all properties used by the plugin
 * The properties defined here must be specified within
 * guacamole.properties to configure the tutorial authentication provider.
 */
// TODO maybe make a config opbject

public class GuacamoleTriggerProperties{

    /**
     * This class should not be instantiated.
     */
    private GuacamoleTriggerProperties() {}

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

    public static final IntegerGuacamoleProperty DISCONECT_TIME =
        new IntegerGuacamoleProperty () {

        @Override
        public String getName() { return "disconect-time"; }

    };

}
