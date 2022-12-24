package org.apache.guacamole.guacamoletrigger.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Circular buffer for most recent (tunnelid,Host)
 */
public class TunnelBuffer {

    private static final Logger logger = LoggerFactory.getLogger(TriggerREST.class);
    int index = 0;
    Tunnel2Host[] tunnel2Hosts;

    TunnelBuffer(int capacity){
         tunnel2Hosts =  new Tunnel2Host[capacity];
    }

    public void push (String tunnelID, Host host){

        logger.error("push tunnel: {} host:{}",tunnelID,host.getHostname());
        tunnel2Hosts[index] = new Tunnel2Host(tunnelID,host.getHostname()) ;
        this.index = ++index % tunnel2Hosts.length;
    }
    /**
     * Get performace a linear search for tunnelID, and return matching Host
     */

    public Host get(String tunnelID) {
        for (Tunnel2Host t2h: tunnel2Hosts ){
            if (t2h != null && t2h.tunnelID.equals(tunnelID)) {
                return Host.findHost(t2h.host);
            }
        }
        return null;
    }
}
/**
 * tunnel2Hosts just mimics a Tuble (tunnelID,Host)
 */

class Tunnel2Host {


  public final String tunnelID;

  public final String host;

  Tunnel2Host(String tunnelID, String host) {
    this.tunnelID = tunnelID;
    this.host = host;
  }
}
