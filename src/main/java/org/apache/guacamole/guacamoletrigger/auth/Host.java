package org.apache.guacamole.guacamoletrigger.auth;

import java.util.concurrent.ScheduledFuture;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import org.apache.guacamole.net.auth.AuthenticatedUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.guacamole.guacamoletrigger.auth.Console;

public class Host  {


    // and we can query that by checking if there is thread running for that.
    public enum hostStatus {
        UNKNOW,
        BOOTING,
        TERMINATING,
    };

    private static ConfigurationService settings = new ConfigurationService();

    private Console console = new Console (
                    line -> logger.debug("stdout: {}", line),
                    line -> logger.info("stderr: {}", line),
                    settings.getGuacamoleHome(),
                    settings.getCommandTimeout()
                );

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> stopping = null;
    private ScheduledFuture<?> starting = null;

    private String hostname;

    // connection is synchronizedSet so it becomse thread save
    private Set<UUID> connections = Collections.synchronizedSet(new HashSet<UUID>()) ;
    private static final Logger logger = LoggerFactory.getLogger(Host.class);

    private static ConcurrentMap<String,Host> hosts = new ConcurrentHashMap<String,Host>();

    public static String Tunnel2HostName(GuacamoleTunnel tunnel) throws GuacamoleUnsupportedException {

        GuacamoleSocket socket = tunnel.getSocket();
        if(!(socket instanceof ConfiguredGuacamoleSocket)){

            throw new GuacamoleUnsupportedException("can't handle unconfigerd sockets");
        }

        GuacamoleConfiguration socketConfig = ((ConfiguredGuacamoleSocket) socket).getConfiguration();
        return socketConfig.getParameter("hostname");
    }

    public static Host findHost (String hostname) {

        return hosts.get(hostname);
    }

    public Host(String hostname ) throws GuacamoleException{

            this.hostname = hostname;

            hosts.put(hostname,this);
    }

    public int openConnections() {
        return connections.size();
    }

    public void addConnection (AuthenticatedUser user, GuacamoleTunnel tunnel) {

        UUID uuid = tunnel.getUUID();
        connections.add(uuid);
        logger.info("connection: {} added. now there are {} conections to host {}.",uuid.toString(), connections.size(), this.hostname);
    }

    public boolean removeConnection(GuacamoleTunnel tunnel) {


        UUID uuid = tunnel.getUUID();
        boolean success = connections.remove(uuid);
        logger.info("connection: {} removed. now there are {} conections to host {}.", uuid.toString(), connections.size(), this.hostname);
        return success;
    }

    public String getHostname (){
         return hostname;
    }

    public hostStatus getStatus (){
        if (isStarting()) {
            return hostStatus.BOOTING;
        }
        if (isStopping()) {
            return hostStatus.TERMINATING;
        }
        return hostStatus.UNKNOW;
    }


    public boolean isStopping(){
        return stopping != null && !stopping.isDone();
    }
    public boolean isStarting(){
        return starting != null &&  ! starting.isDone();
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
    public void scheduleStop(AuthenticatedUser authUser) throws GuacamoleException {

        Integer shutdownDelay = settings.getShutdownDelay();

        // if stopping is already scheduled, don't schedule another one
        if (isStopping()){
            return;
        }

        logger.info("schedule stop command for host {}", this.hostname);

        stopping = executor.schedule(new Runnable() {
            @Override
            public void run() {

                stop(authUser);

            }
        }, shutdownDelay, TimeUnit.SECONDS);
    }

    private void stop(AuthenticatedUser authUser) {
        String command = "";
        try{
            command = settings.getStopCommand();
        } catch (GuacamoleException e) {
            logger.info("no stop command provide. skip stopping: {}", this.hostname);
            return;
        }

        String guacamoleUsername = authUser.getCredentials().getUsername();

        Map<String,String> commandEnvironment = new HashMap<String,String>();
        commandEnvironment.put("guacamoleUsername", guacamoleUsername);
        commandEnvironment.put("hostname", hostname);

        logger.info("{}> {}", this.hostname, command);

        if (starting != null) {

            logger.error("Host stop command is run while host is still booting");
        }


        int exitCode = console.run(command ,commandEnvironment);
        if (exitCode != 0){
            logger.error("stop command for {}, failed with exit code {}",hostname, exitCode );
        }

        // remove reference to host. maybe this is overkill
        if (starting == null || starting.isDone()) {
            hosts.remove(hostname);
            executor.shutdown();
        }

    }

    /**
     * lazyStart will try to start Host if (not already booting) (tunnel is not open for a while)
     */
    public void lazyStart(GuacamoleTunnel tunnel,AuthenticatedUser authUser) {

        if (isStarting()){
            return ;
        }

        // connection starts open. but will get closed eventually if Host can't be reached
        // so waith a bit. so guacd gets time to detect host is unreachable and close the tunnel

        int startUpDellay = 2000; // TODO make this configurable

        // if Tunnel is already closed, try to start host direct
        if (!tunnel.isOpen()) {
            startUpDellay = 0;
        }

        // if stop command is already running, it will start after stopcommand has finished
        starting = executor.schedule(new Runnable() {
            @Override
            public void run() {

                // host still is unreachable start it
                if (! tunnel.isOpen()){
                    start(authUser);
                }
            }
        }, startUpDellay, TimeUnit.MILLISECONDS);
    }

    public void cancelStop(){
        if (stopping != null &&  !stopping.isCancelled()) {
            stopping.cancel(false);
            logger.info("canceld schedule stop command for host {}", this.hostname);

        }
    }

    private void start(AuthenticatedUser authUser) {

        String command = "";
        try{
            command = settings.getStartCommand();
        } catch (GuacamoleException e) {
            logger.info("no start command provide. skip starting: {}", this.hostname);
            return;
        }

        String guacamoleUsername = authUser.getCredentials().getUsername();

        Map<String,String> commandEnvironment = new HashMap<String,String>();
        commandEnvironment.put("hostname", hostname);
        commandEnvironment.put("guacamoleUsername", guacamoleUsername);

        console.clear();

        logger.info("{}@{}> {}", guacamoleUsername,this.hostname, command);
        int exitCode = console.run(command ,commandEnvironment);
        if (exitCode != 0){
            logger.error("start command for {}@{}, failed with exit code {}",guacamoleUsername, hostname, exitCode );
        }
    }
}
