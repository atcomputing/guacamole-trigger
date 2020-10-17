package org.apache.guacamole.guacamoletrigger.auth;


public class TunnelBuffer {

    int index = 0;
    Tunnel2Host[] tunnel2Hosts;

    TunnelBuffer(int capacity){
         tunnel2Hosts =  new Tunnel2Host[capacity];
    }

    public void push (String tunnelID, Host host){
        tunnel2Hosts[index] = new Tunnel2Host(tunnelID,host) ;
        this.index = ++index % tunnel2Hosts.length;
    }

    public Host get(String tunnelID) {
        for (Tunnel2Host t2h: tunnel2Hosts ){
            if (t2h != null && t2h.tunnelID.equals(tunnelID)) {
                return t2h.host;
            }
        }
        return null;
    }
}

class Tunnel2Host {
  public final String tunnelID;
  public final Host host;
  Tunnel2Host(String tunnelID, Host host) {
    this.tunnelID = tunnelID;
    this.host = host;
  }
}
