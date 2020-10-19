package org.apache.guacamole.guacamoletrigger.auth;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.guacamoletrigger.auth.Host;
import org.apache.guacamole.guacamoletrigger.auth.TriggerREST;
import org.apache.guacamole.net.GuacamoleTunnel;

public class TriggerUserContext extends AbstractUserContext {


    private static final Logger logger = LoggerFactory.getLogger(TriggerUserContext.class);

    /**
     * The unique identifier of the root connection group.
     */

    public static final String ROOT_IDENTIFIER = DEFAULT_ROOT_CONNECTION_GROUP;


    private static ConcurrentMap<String,TunnelBuffer> user2TunnelBuffer = new ConcurrentHashMap<String,TunnelBuffer>();

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
        user2TunnelBuffer.put(username,new TunnelBuffer(10));
    }
    protected void finalize(){

        user2TunnelBuffer.put(self.getIdentifier(),null);

    }


    public static void registerConnection (AuthenticatedUser authUser, GuacamoleTunnel tunnel) throws GuacamoleException {

        String tunnelID = tunnel.getUUID().toString();
        TunnelBuffer tunnelBuffer = user2TunnelBuffer.get(authUser.getCredentials().getUsername());
        Host registeredHost = tunnelBuffer.get(tunnelID);
        if (registeredHost == null){
          registeredHost =  Host.getHost(authUser,tunnel);
          registeredHost.start(authUser); // TODO only when not running?
          tunnelBuffer.push(tunnelID, registeredHost);
        }
    }

    public static void deregisterConnection (AuthenticatedUser user, GuacamoleTunnel tunnel) throws GuacamoleException {
        Host registeredHost = Host.findHost(tunnel);

        if (registeredHost != null) {
            registeredHost.removeConnection(tunnel); // TODO tunnelid
            if (registeredHost.openConnections() <= 0 ){ // TODO do this in Host?
                registeredHost.scheduleStop();
            }
        } else {
            String tunnelID = tunnel.getUUID().toString();
            logger.error("Can`t close unknow tunnel: " + tunnelID  );
        }
    }

    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return new SimpleDirectory<Connection>();
    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return new TriggerREST(user2TunnelBuffer.get(self().getIdentifier()));
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }
}

