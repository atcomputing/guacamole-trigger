package org.apache.guacamole.guacamoletrigger.event;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.listener.Listener;

import org.apache.guacamole.guacamoletrigger.auth.TriggerUserContext;

public class GuacamoleTrigger implements Listener {


    public GuacamoleTrigger() throws GuacamoleException{
    }

    @Override
    public void handleEvent(Object event) throws GuacamoleException{

        if (event instanceof TunnelConnectEvent) {
            TunnelConnectEvent tcEvent = (TunnelConnectEvent) event;
            TriggerUserContext.registerConnection(tcEvent.getAuthenticatedUser(), tcEvent.getTunnel());
        }else if(event instanceof TunnelCloseEvent) {
            TunnelCloseEvent tcEvent = (TunnelCloseEvent) event;
            // TODO no problems with concurency
            // TriggerUserContext.deregisterConnection(tcEvent.getAuthenticatedUser(), tcEvent.getTunnel());
        }
    }

}
