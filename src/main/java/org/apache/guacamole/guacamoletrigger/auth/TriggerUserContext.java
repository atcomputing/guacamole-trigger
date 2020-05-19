package org.apache.guacamole.guacamoletrigger.auth;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.UUID;

import org.apache.guacamole.net.auth.AbstractUserContext;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.simple.SimpleObjectPermissionSet;
import org.apache.guacamole.net.auth.simple.SimpleUser;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.guacamoletrigger.auth.Host;
import org.apache.guacamole.guacamoletrigger.auth.TriggerREST;
import org.apache.guacamole.net.GuacamoleTunnel;

public class TriggerUserContext extends AbstractUserContext {

    private static ConcurrentMap<String,Host> hosts = new ConcurrentHashMap<String,Host>();
    /**
     * The unique identifier of the root connection group.
     */

    public static final String ROOT_IDENTIFIER = DEFAULT_ROOT_CONNECTION_GROUP;

    /**
     * The AuthenticationProvider that created this UserContext.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Reference to the user whose permissions dictate the configurations
     * accessible within this UserContext.
     */
    private final User self;

    /**
     * Construct a GuacamoleTriggerUserContext using the authProvider and
     * the username.
     *
     * @param authProvider
     *     The authentication provider module instantiating this
     *     this class.
     *
     * @param username
     *     The name of the user logging in that will be associated
     *     with this UserContext.
     *
     * @throws GuacamoleException
     *     If errors occur initializing the ConnectionGroup,
     *     ConnectionDirectory, or User.
     */
    public TriggerUserContext(AuthenticationProvider authProvider,
            String username) throws GuacamoleException {


        // Initialize the user to a SimpleUser with the provided username,
        // no connections, and the single root group.
        this.self = new SimpleUser(username) {

            @Override
            public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
                return new SimpleObjectPermissionSet(getConnectionGroupDirectory().getIdentifiers());
            }
            @Override
            public ObjectPermissionSet getConnectionGroupPermissions() throws GuacamoleException {
                return new SimpleObjectPermissionSet(Collections.singleton(ROOT_IDENTIFIER));
            }

        };

        // Set the authProvider to the calling authProvider object.
        this.authProvider = authProvider;

    }

    public static void registerConnection (AuthenticatedUser authUser, GuacamoleTunnel tunnel) throws GuacamoleException {

        String tunnelID = tunnel.getUUID().toString();
        Host registeredHost = hosts.get(tunnelID);

        if (registeredHost == null) {
            registeredHost =  Host.getHost(authUser,tunnel);
            registeredHost.start(authUser);
        } else {
           registeredHost.addTunnel(authUser,tunnel);
        }
        hosts.put(tunnelID,registeredHost);
    }

    public static void deregisterConnection (AuthenticatedUser user, GuacamoleTunnel tunnel){
        UUID tunnelID = tunnel.getUUID();
        Host registeredHost = hosts.get(tunnelID);

        if (registeredHost != null) {
            registeredHost.removeTunnel(tunnel);
            hosts.remove(tunnel.getUUID());
            // TODO check if there are no longer any connection. andy maybe stop vm
        }

    }

    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return new SimpleDirectory();
    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return new TriggerREST(hosts, self());
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

}

