package pt.tecnico.distledger.namingserver.domain.entry;

import java.util.Set;
import java.util.HashSet;

public class ServiceEntry {
    
    private String service;

    private Set<ServerEntry> serverEntries;

    public ServiceEntry(String service) {
        this.service = service;
        this.serverEntries = new HashSet<ServerEntry>();
    }

    public String getService() {
        return service;
    }

    public Set<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    public void addServerEntry(ServerEntry server){
        serverEntries.add(server);
    }
    
}
