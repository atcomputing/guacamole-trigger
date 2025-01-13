package org.apache.guacamole.guacamoletrigger.auth;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.guacamole.net.auth.User;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.guacamoletrigger.auth.Host;
import org.apache.guacamole.guacamoletrigger.auth.TunnelBuffer;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces(MediaType.APPLICATION_JSON)
public class TriggerREST {


    private static final Logger logger = LoggerFactory.getLogger(TriggerREST.class);
    private TunnelBuffer tunnelBuffer ;

    public TriggerREST(TunnelBuffer tunnelBuffer){
            this.tunnelBuffer = tunnelBuffer;
    }


    // these setting might become connection specific instead of global
    @GET
    @Path("config")
    public Map<String, Integer> getConfig() throws GuacamoleException {

        ConfigurationService settings= new ConfigurationService();
        Map<String,Integer> anser = new HashMap<String,Integer>();

        anser.put("disconnectTime",settings.getDisconnectTime());
        anser.put("idleTime",settings.getIdleTime());
        return anser;
    }

    @GET
    @Path("host/{tunnelID}")
    public Map<String, String> getHostInfo(@PathParam("tunnelID")String tunnelID ) throws GuacamoleException {

        ConfigurationService settings= new ConfigurationService();
        Host host = tunnelBuffer.get(tunnelID);

        if (host != null ){


            Map<String,String> anser = new HashMap<String,String>();
            anser.put("hostname",host.getHostname());
            anser.put("status",host.getStatus().name());
            anser.put("console",host.getConsole());
            anser.put("consoleTitle", settings.getConsoleTitle().replaceAll("\\$hostname", host.getHostname()));

            logger.info("Rest {}: {} {} ", tunnelID, host.getHostname(), host.getStatus().name()   );
            return anser;
        }

        logger.info("Can`t give hostInfo for: " + tunnelID  );
        return null;
    }
}

