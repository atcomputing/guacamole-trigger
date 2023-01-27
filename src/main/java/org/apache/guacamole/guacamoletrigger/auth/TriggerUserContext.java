package org.apache.guacamole.guacamoletrigger.auth;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;

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

import org.apache.guacamole.guacamoletrigger.auth.Host;
import org.apache.guacamole.guacamoletrigger.auth.TriggerREST;

public class TriggerUserContext extends AbstractUserContext {


    private static final Logger logger = LoggerFactory.getLogger(TriggerUserContext.class);

    /**
     * The unique identifier of the root connection group.
     */

    public static final String ROOT_IDENTIFIER = DEFAULT_ROOT_CONNECTION_GROUP;


    // We can`t acces specific usercontext from tunnelevent "org/apache/guacamole/guacamoletrigger/event/GuacamoleTrigger.java"
    // So we make registerConnection/deregisterConnection static functions. that modify static map from user to tunnel data
    // normal member functions, have use this map to get only tunnel data for there user.

    // The Tunnel data that we use is list of most recent (tunnelId, Host)
    // The webfront does not know when it creates a connection to which host it is.
    // it can use TriggerREST query Host status via tunnelID
    // That is why we need to konw mapping tunnelID Host
    // We need most recent, because multiple connections can be queried same time
    // if you store most recent you can answer those query's for at least for new connection (host that start up)

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

        // stores for this user the 10 most recent (tunnelID,host)
        // This means you can start 10 connection per user,at the same time where you still get correct boot messags
        user2TunnelBuffer.put(username,new TunnelBuffer(10));
    }
    protected void finalize(){

        // not the cleanest sollution, guacamole keeps userContext around.so this might never be called.

        // remove tunnel data from static map because we no longer need it, and this way it can be garbaged collected
        user2TunnelBuffer.put(self.getIdentifier(),null);

    }

    public static void registerConnection (AuthenticatedUser authUser, GuacamoleTunnel tunnel) throws GuacamoleException {

        String tunnelID = tunnel.getUUID().toString();
        // if (tunnelID == null) {
        //     logger.error("could not get a tunnelID");
        //     tunnelID= "dummy";
        //
        // }
        // String userName = authUser.getCredentials().getUsername();
        String userName = authUser.getAuthenticationProvider().getUserContext(authUser).self().getIdentifier();

        if (userName == null){

            logger.info("could not get a username");
            userName =  AuthenticatedUser.ANONYMOUS_IDENTIFIER;
        }

        TunnelBuffer tunnelBuffer = user2TunnelBuffer.get(userName);
        if (tunnelBuffer == null){
            // user context never initialized, by some auth provider
            tunnelBuffer = new TunnelBuffer(10);
            user2TunnelBuffer.put(userName,tunnelBuffer);
        }
        if (tunnelBuffer.get(tunnelID)!= null){
            // FIXME
            logger.error("tunnelID {} is registerd more then once" , tunnelID );
        }

        String hostname  = Host.Tunnel2HostName(tunnel);

        Host host = Host.findHost(hostname);

        if (host == null) {
            host = new Host( hostname);
        }

        host.addConnection(userName, tunnel);

        // from this point onward webclient can query host status.
        tunnelBuffer.push(tunnelID, host);

        host.cancelStop();
        host.lazyStart(tunnel,userName);

        tunnelBuffer.push(tunnelID, host);
    }

    public static void deregisterConnection (AuthenticatedUser  authUser, GuacamoleTunnel tunnel) throws GuacamoleException {

        String userName = authUser.getAuthenticationProvider().getUserContext(authUser).self().getIdentifier();
        String hostname  = Host.Tunnel2HostName(tunnel);

        Host registeredHost = Host.findHost(hostname);

        if (registeredHost != null) {

            // removeConnection is thread save. so if 1 connnection is removed 2 times at the same time it will return true ones
            if (registeredHost.removeConnection(tunnel) && registeredHost.openConnections() <= 0 ){

                // stop is scheduled instead of running it directly. to prevent the situation where:
                // If a connection fails, and it`s the only connection to a host.
                // the connection count becomes 0, a stop command is run. guacamole succeeds in reconnecting.
                // but you still lose connection, because you have turned off you host

                // But if you schedule your Stop in near feature. you can cancel that if you try to reconnect.
                // This also means if you can't boot and connect in the time (GuacamoleTriggerProperties.SHUTDOWN_DELAY)
                // Then stopcomand will be run immediately after startup command
                registeredHost.scheduleStop(userName);
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

