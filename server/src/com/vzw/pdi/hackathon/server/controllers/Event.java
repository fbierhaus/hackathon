/**
 * 
 */
package com.vzw.pdi.hackathon.server.controllers;

import net.sf.serfj.RestController;
import net.sf.serfj.annotations.GET;



/**
 * @author fred
 *
 */
public class Event extends RestController {

    @GET
    public com.vzw.pdi.hackathon.data.Event show() {
        // Gets ID from URL /banks/1
        String id = this.getId();
                     
        // By default, this action redirects to show.jsp (or show.html or show.htm)
        
        com.vzw.pdi.hackathon.data.Event event = new com.vzw.pdi.hackathon.data.Event();
        event.setId(id);
        
        return event;
    }
    
    
}
