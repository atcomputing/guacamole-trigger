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
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;

@Produces(MediaType.APPLICATION_JSON)
public class TriggerREST {

    private Map<String,Host> hosts;

    private User user;

    public TriggerREST(Map<String,Host> hosts, User user){
            this.hosts = hosts;
            this.user = user;
    }


    @GET
    @Path("config")
    public Map<String, Integer> getConfig() throws GuacamoleException {

        Environment settings= new LocalEnvironment();
        Map<String,Integer> anser = new HashMap<String,Integer>();
        anser.put("disconectTime",settings.getProperty(GuacamoleTriggerProperties.DISCONECT_TIME, 3600));
        anser.put("idleTime",settings.getProperty(GuacamoleTriggerProperties.IDLE_TIME, 1800));
        return anser;
    }

    @GET
    @Path("host/{tunnelID}")
    public Map<String, String> getHost(@PathParam("tunnelID")String tunnelID ) throws GuacamoleException {
        Host host = hosts.get(tunnelID);

        if (host != null && host.owner(tunnelID).getIdentifier() == user.getIdentifier() ){

            Map<String,String> anser = new HashMap<String,String>();
            anser.put("hostname",host.getHostname());
            anser.put("status",host.getStatus());
            anser.put("console",host.getConsole());
            return anser;
        }
        return null;
    }
}

