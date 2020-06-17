package org.apache.guacamole.guacamoletrigger.auth;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.ScheduledFuture;

import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.AuthenticatedUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.guacamoletrigger.auth.Console;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

public class Host  {


    // TODO this more information, then we use. we only need to know if its is booting, or stopping
    // and we can query that by checking if there is thread running for that.
    enum hostStatus {
        UNKNOW,
        BOOTING,
        RUNNING,
        TERMINATED,
        // FAILTOBOOT
    };


    private Console console;
    private ScheduledFuture<?> shutdown;
    private hostStatus status = hostStatus.UNKNOW;
    private String hostname;

    private static Environment settings;
    private static final Logger logger = LoggerFactory.getLogger(Host.class);

    private static ConcurrentMap<String,Host> hosts = new ConcurrentHashMap<String,Host>();
    private ConcurrentMap<String,AuthenticatedUser> tunnels ;

    // this is used for createing environment variable for starting en stoping command
    // TODO do we need all of this? there can be more then 1 config for a host. mybe only use hostname
    private GuacamoleConfiguration socketConfig;

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

        tunnels.remove(tunnel.getUUID().toString());
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

    public boolean ping() {

        boolean reachable = false;
        try{
            reachable = InetAddress.getByName(this.hostname).isReachable(100);
        } catch (Exception e){

            logger.info("could not ping {}", this.hostname);
        }
        return reachable;
    }
    public void scheduleStop() throws GuacamoleException {

        String command = settings.getProperty(GuacamoleTriggerProperties.STOP_COMMAND);
        if (command == null){ ;

            logger.info("no stop command provide. dont schedule stopping: {}", this.hostname);
            return;
        }

        Map<String,String> commandEnvironment = socketConfig.getParameters();

            if (shutdown == null){

                // TODO do we need console for this
                console = new Console (
                    line -> logger.debug("stdout: {}", line),
                    line -> logger.info("stderr: {}", line)
                );
                shutdown = Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                    @Override
                    public void run() {
                        logger.info("cmd: {}, status:{}", command,status.name());
                        int exitCode = console.run(command ,commandEnvironment);
                        if (exitCode == 0){
                            status = hostStatus.TERMINATED;
                        } else {
                            status = hostStatus.UNKNOW;
                        }
                    }
                },30,TimeUnit.SECONDS); // TODO make configerable
            }
            // TODO can terminated host be removed from hosts?
            // and what if there is no shutdown
    }

    public int openTunnels() {
        return tunnels.size();
    }

    public void start (AuthenticatedUser authUser) throws GuacamoleException {

        if (shutdown != null) {
            shutdown.cancel(false);
            shutdown = null;
        }

        String command = settings.getProperty(GuacamoleTriggerProperties.START_COMMAND);
        if (command == null){ ;

            logger.info("no start command provide. skip starting: {}", this.hostname);
            return;
        }

        // TODO test for ping is not generic solution, maybe should includ in command or make optional

        if (! ping()){

            String guacamoleUsername = authUser.getCredentials().getUsername();
            Map<String,String> commandEnvironment = socketConfig.getParameters();
            commandEnvironment.put("guacamoleUsername", guacamoleUsername);

            if (status != hostStatus.BOOTING){

                status = hostStatus.BOOTING;
                console = new Console (
                    line -> logger.debug("stdout: {}", line),
                    line -> logger.info("stderr: {}", line)
                );
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {

                        logger.info("cmd: {}, status:{}", command,status.name());
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
