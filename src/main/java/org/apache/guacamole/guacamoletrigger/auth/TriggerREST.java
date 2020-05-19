package org.apache.guacamole.guacamoletrigger.auth;

import java.util.UUID;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.GET;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Produces;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.User;

import org.apache.guacamole.guacamoletrigger.auth.Host;

@Produces(MediaType.APPLICATION_JSON)
public class TriggerREST {

    private Map<String,Host> hosts;

    private User user;

    public TriggerREST(Map<String,Host> hosts, User user){
            this.hosts = hosts;
            this.user = user;
    }

    @GET
    @Path("host/{tunnelID}")
    public Map<String, String> getHost(@PathParam("tunnelID")String tunnelID ) throws GuacamoleException {
        Host host = hosts.get(tunnelID);
        Map<String,String> anser = new HashMap<String,String>();

        if (host != null && host.owner(tunnelID).getIdentifier() == user.getIdentifier() ){

            anser.put("hostname",host.getHostname());
            anser.put("status",host.getStatus());
            anser.put("console",host.getConsole());
            return anser;
        }
        return null;
    }
}

