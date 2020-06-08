package org.apache.guacamole.guacamoletrigger.auth;

import java.io.InputStreamReader ;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.guacamoletrigger.auth.Console;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Host  {

    enum hostStatus {
        UNKNOW,
        BOOTING,
        RUNNING
        // FAILTOBOOT
        // TERMINATED
    };

    // private ReentrantLock lock = new ReentrantLock();

    private Console console;
    private hostStatus status = hostStatus.UNKNOW;
    private String hostname;

    private static Environment settings;
    private static final Logger logger = LoggerFactory.getLogger(Host.class);

    private static ConcurrentMap<String,Host> hosts = new ConcurrentHashMap<String,Host>();
    private ConcurrentMap<String,AuthenticatedUser> tunnels ;

    private GuacamoleConfiguration socketConfig; //TODO

    public static Host getHost(AuthenticatedUser authUser,GuacamoleTunnel tunnel) throws GuacamoleUnsupportedException, GuacamoleException {

        settings = new LocalEnvironment();
        GuacamoleSocket socket = tunnel.getSocket();
        if(!(socket instanceof ConfiguredGuacamoleSocket)){

            throw new GuacamoleUnsupportedException("can't handle unconfigerd sockets");
        }

        GuacamoleConfiguration socketConfig = ((ConfiguredGuacamoleSocket) socket).getConfiguration();
        String hostname = socketConfig.getParameter("hostname");
        Host host = hosts.get(hostname);

        if (host == null) {
            host = new Host(authUser,tunnel.getUUID().toString(), socketConfig);
            hosts.put(hostname,host);
        } else {
            host.addTunnel(authUser, tunnel);
        }

        return host;

    }
    private Host(AuthenticatedUser user,String tunnelID,GuacamoleConfiguration socketConfig) throws GuacamoleUnsupportedException, GuacamoleException {

            this.socketConfig = socketConfig;
            this.hostname = socketConfig.getParameter("hostname");
            // This is for the future, to be able to check howmany connection use this host. and if it can be truned off.
            tunnels = new ConcurrentHashMap<String,AuthenticatedUser>();
            tunnels.put(tunnelID,user);

    }

    public void addTunnel(AuthenticatedUser user, GuacamoleTunnel tunnel) {

        tunnels.put(tunnel.getUUID().toString(), user);
    }

    public void removeTunnel(GuacamoleTunnel tunnel) {

        tunnels.remove(tunnel.getUUID());
    }

    public AuthenticatedUser owner(String tunnelID){

        return tunnels.get(tunnelID);
    }

    public String getHostname (){
         return hostname;
    }

    public String getStatus (){
         return status.name();
    }
    public String getConsole(){
        if (console != null){
            return console.getBufferOutput();
        } else {
            return "No output yet";
        }
    }

    public void start (AuthenticatedUser authUser) throws GuacamoleException {

        String command = settings.getProperty(GuacamoleTriggerProperties.START_COMMAND);
        if (command != null){ ;

            // TODO test for ping is not generic solution, maybe should includ in command or make optional
            boolean reachable = false;
            try{
                reachable = InetAddress.getByName(this.hostname).isReachable(100);
            } catch (Exception e){

                logger.info("could not ping {}", this.hostname);
            }

            if (! reachable){

                String guacamoleUsername = authUser.getCredentials().getUsername();
                Map<String,String> commandEnvironment = socketConfig.getParameters();
                commandEnvironment.put("guacamoleUsername", guacamoleUsername);

                if (status != hostStatus.BOOTING){

                    status = hostStatus.BOOTING;
                    logger.info("cmd: {}, status:{}", command,status.name());

                            console = new Console (
                                line -> logger.debug("stdout: {}", line)
                                ,line -> logger.info("stderr: {}", line)
                            );
                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            int exitCode = console.run(command ,commandEnvironment);
                            if (exitCode == 0){
                                status = hostStatus.RUNNING;
                            } else {
                                status = hostStatus.UNKNOW;
                            }
                        }
                    });
                }
            }
        }
    }
}
