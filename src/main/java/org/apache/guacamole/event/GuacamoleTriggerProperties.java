package org.apache.guacamole.event;

import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Utility class containing all properties used by the plugin
 * The properties defined here must be specified within
 * guacamole.properties to configure the tutorial authentication provider.
 */
public class GuacamoleTriggerProperties{

    /**
     * This class should not be instantiated.
     */
    private GuacamoleTriggerProperties() {}

    /**
     * The only user to allow.
     */
    public static final StringGuacamoleProperty START_COMMAND =
        new StringGuacamoleProperty() {

        @Override
        public String getName() { return "start-command"; }

    };
}
