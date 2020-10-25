package org.apache.guacamole.guacamoletrigger.auth;

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


    // TODO TERMINATED is not used, and UNKNOW,and RUNNING are factional equivalent, besides log messages
    // and we can query that by checking if there is thread running for that.
    public enum hostStatus {
        UNKNOW,
        BOOTING,
        RUNNING,
        TERMINATED,
    };

    private Console console = new Console (
                    line -> logger.debug("stdout: {}", line),
                    line -> logger.info("stderr: {}", line)
                );

    private ScheduledFuture<?> shutdown;
    private hostStatus status = hostStatus.UNKNOW;
    private String hostname;
    private int connections = 0; //TODO race condition
    private static Environment settings;
    private static final Logger logger = LoggerFactory.getLogger(Host.class);

    private static ConcurrentMap<String,Host> hosts = new ConcurrentHashMap<String,Host>();

    // this is used for createing environment variable for starting en stoping command
    // TODO do we need all of this? there can be more then 1 config for a host. mybe only use hostname
    private GuacamoleConfiguration socketConfig;


    public static Host findHost (GuacamoleTunnel tunnel)throws GuacamoleUnsupportedException{

        GuacamoleSocket socket = tunnel.getSocket();
        if(!(socket instanceof ConfiguredGuacamoleSocket)){

            throw new GuacamoleUnsupportedException("can't handle unconfigerd sockets");
        }

        GuacamoleConfiguration socketConfig = ((ConfiguredGuacamoleSocket) socket).getConfiguration();
        String hostname = socketConfig.getParameter("hostname");

        return hosts.get(hostname);
    }

    public static Host getHost(AuthenticatedUser authUser,GuacamoleTunnel tunnel) throws GuacamoleUnsupportedException, GuacamoleException {

        // TODO remove not neeted when we dont use socket config
        GuacamoleSocket socket = tunnel.getSocket();
        if(!(socket instanceof ConfiguredGuacamoleSocket)){

            throw new GuacamoleUnsupportedException("can't handle unconfigerd sockets");
        }

        GuacamoleConfiguration socketConfig = ((ConfiguredGuacamoleSocket) socket).getConfiguration();
        Host host = findHost(tunnel);
        if (host == null) {
            settings = new LocalEnvironment(); // TODO do we have to reinitalise static variable
            host = new Host(authUser,tunnel, socketConfig);

        } else {

            host.addConnection(authUser, tunnel);
        }

        return host;

    }
    private Host(AuthenticatedUser user,GuacamoleTunnel tunnel,GuacamoleConfiguration socketConfig) throws GuacamoleUnsupportedException, GuacamoleException {

            this.socketConfig = socketConfig;
            this.hostname = socketConfig.getParameter("hostname");

            hosts.put(hostname,this);
            addConnection(user,tunnel);
    }

    public int openConnections() {
        return connections;
    }

    public void addConnection (AuthenticatedUser user, GuacamoleTunnel tunnel) {

        connections++;
        // cancel shutdown. or make cancel shutdown a sepearte method
        logger.info("connection: {} added. now there are {} conections to host {}.", tunnel.getUUID().toString(), connections, this.hostname);
    }

    public void removeConnection(GuacamoleTunnel tunnel) {

        connections--;
        if (connections < 0) {

            logger.error("connection count for {} reached {}",  this.hostname, connections);
            connections = 0;
        }

        logger.info("connection: {} removed. now there are {} conections to host {}.", tunnel.getUUID().toString(), connections, this.hostname);
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
            return "No console output";
        }
    }

    /**
     * schedule a stop command for this Host in GuacamoleTriggerProperties.SHUTDOWN_DELAY seconds
     *
     * if a second stop command is scheduled while the first still has not finished, the second will be ignored
     */

    public void scheduleStop() throws GuacamoleException {

        String command = settings.getProperty(GuacamoleTriggerProperties.STOP_COMMAND);
        Integer shutdownDelay = settings.getProperty(GuacamoleTriggerProperties.SHUTDOWN_DELAY, 300);
        if (command == null){ ;

            logger.info("no stop command provide. dont schedule stopping: {}", this.hostname);
            return;
        }

        Map<String,String> commandEnvironment = socketConfig.getParameters();

        // if shutdown is already scheduled, don't schedule another one
        if (shutdown != null){
            return;
        }

        logger.info("schedule stop command for host {}", this.hostname);

        shutdown = Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
            @Override
            public void run() {


                logger.info("cmd: {}, status:{}", command,status.name());

                if (status == hostStatus.BOOTING) {

                    logger.error("Host stop command is run while host is still booting");
                }

                int exitCode = console.run(command ,commandEnvironment);
                if (exitCode == 0){
                    status = hostStatus.TERMINATED;
                } else {
                    status = hostStatus.UNKNOW;
                    logger.error("stop command for {}, failed with exit code {}",hostname, exitCode );
                }

                shutdown=null;

                // TODO can terminated host be removed from hosts?
            }
        }, shutdownDelay, TimeUnit.SECONDS);
    }

    /**
     * lazyStart will try to start Host if (not already booting) (tunnel is not open for a while)
     */
    public void lazyStart   (GuacamoleTunnel tunnel,AuthenticatedUser authUser) {


        if (shutdown != null) {
            shutdown.cancel(false);
            shutdown = null;
            logger.info("canceld schedule stop command for host {}", this.hostname);
        }
        if (status != hostStatus.BOOTING){
            status = hostStatus.BOOTING;

            // connection starts open. but will get closed eventually if Host can't be reached
            // so waith a bit. so guacd gets time to detect host is unreachable and close the tunnel

            int startUpDellay = 2000;

            // if Tunnel is already closed, try to start host direct
            if (!tunnel.isOpen()) {
                startUpDellay = 0;
            }

            Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                    @Override
                    public void run() {

                        // host still is unreachable start it
                        if (! tunnel.isOpen()){
                            start(authUser);
                        }else {
                            // host was reachable
                            status = hostStatus.RUNNING;
                        }
                    }
                }, startUpDellay, TimeUnit.MILLISECONDS);
        }
    }
    public void start (AuthenticatedUser authUser) {

        if (shutdown != null) {
            shutdown.cancel(false);
            shutdown = null;
            logger.info("canceld schedule stop command for host {}", this.hostname);
        }

        String command = "";
        try{
            command = settings.getProperty(GuacamoleTriggerProperties.START_COMMAND);
        } catch (GuacamoleException e) {
            logger.info("no start command provide. skip starting: {}", this.hostname);
            return;
        }

        String guacamoleUsername = authUser.getCredentials().getUsername();
        Map<String,String> commandEnvironment = socketConfig.getParameters();
        commandEnvironment.put("guacamoleUsername", guacamoleUsername);

        status = hostStatus.BOOTING;
        console.clear();

        logger.info("{}@{} {}: {}", guacamoleUsername,this.hostname, status.name(), command);
        int exitCode = console.run(command ,commandEnvironment);
        if (exitCode == 0){
            status = hostStatus.RUNNING;
        } else {
            status = hostStatus.UNKNOW;
            logger.error("start command for {}@{}, failed with exit code {}",guacamoleUsername, hostname, exitCode );
        }
    }
}
