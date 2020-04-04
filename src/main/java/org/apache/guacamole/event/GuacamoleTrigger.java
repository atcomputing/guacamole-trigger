package org.apache.guacamole.event;

import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader ;
import java.io.InputStream;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Collection;
import org.apache.guacamole.form.*;
import org.apache.guacamole.net.auth.*;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.listener.Listener;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.protocols.ProtocolInfo;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;

import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Listener that logs authentication success and failure events.
 */
public class GuacamoleTrigger implements Listener {

    private static final Logger logger =
        LoggerFactory.getLogger(GuacamoleTrigger.class);

    private Environment settings;

    public GuacamoleTrigger() throws GuacamoleException{
        this.settings = new LocalEnvironment();
    }

    @Override
    public void handleEvent(Object event) throws GuacamoleException{

        if (event instanceof TunnelConnectEvent) {
            handleTunnelConnectEvent((TunnelConnectEvent)event);
        }else if(event instanceof TunnelConnectEvent) {
            // TODO logic for stop
        }
    }

    private void handleTunnelConnectEvent(TunnelConnectEvent tcEvent) throws GuacamoleException{


        String command = settings.getProperty(GuacamoleTriggerProperties.START_COMMAND);
        if (command == null){
            logger.info("start-command not defined, skipping");
        } else {

            String guacamoleUsername = tcEvent.getCredentials().getUsername();
            GuacamoleSocket socket = tcEvent.getTunnel().getSocket();

            AuthenticatedUser authUser = tcEvent.getAuthenticatedUser();
            UserContext userContext = authUser.getAuthenticationProvider().getUserContext(authUser);
            // Directory<ActiveConnection> activeConnections = userContext.getActiveConnectionDirectory();
            //
            //
            // logger.info(activeConnections.getIdentifiers().toString());
            // logger.info(tcEvent.getTunnel().toString());


            userContext.getConnectionAttributes().forEach ((f )->{
                logger.info("protocol {} ",f.getFields());
                });

            if(!(socket instanceof ConfiguredGuacamoleSocket)){
                logger.error("can't handle unconfigerd sockets");
                return;
            }

            GuacamoleConfiguration socketConfig = ((ConfiguredGuacamoleSocket) socket).getConfiguration();


            // TODO this makes plugin less generic. and not alway applicable, make optional?
            // test host is reachable
            boolean reachable = false;
            String host = socketConfig.getParameter("hostname");
            try{
                reachable = InetAddress.getByName(host).isReachable(100);
            } catch (Exception e){

                logger.info("could not ping {}", host);
            }

            if (! reachable){
                Map<String,String> commandEnvironment = socketConfig.getParameters();
                commandEnvironment.put("guacamoleUsername", guacamoleUsername);
                runCommand (command,commandEnvironment);
            }

        }

    }

    private int runCommand(String command,Map<String,String> environment){

        logger.info(command);
        ProcessBuilder builder = new ProcessBuilder();
        if ( System.getProperty("os.name").toLowerCase().contains("win")) { // is windows
            builder.command("cmd.exe", "/c", "dir");
        } else {
            builder.command("sh", "-c", command);
        }
        builder.environment().putAll(environment);

        try {

            Process process = builder.start();
            StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream() );
            Executors.newSingleThreadExecutor().submit(stdoutGobbler);

            StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream());
            Executors.newSingleThreadExecutor().submit(stderrGobbler);

            return process.waitFor();
        } catch (IOException e){
            logger.error("could not start: {}\n{}", command, e.getMessage());
        } catch (InterruptedException e) {
            logger.error("command interupted: {} \n{}", command, e.getMessage());
        }
        return 0;
    }


    private class StreamGobbler implements Runnable {
        private InputStream inputStream;
        // private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
            // this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                .forEach((line) -> {logger.info("run:{}", line);});
        }
    }
}
