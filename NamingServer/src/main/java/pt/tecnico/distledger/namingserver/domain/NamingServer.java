package pt.tecnico.distledger.namingserver.domain;

import java.util.List;

import pt.tecnico.distledger.namingserver.domain.entry.ServerEntry;
import pt.tecnico.distledger.namingserver.domain.entry.ServiceEntry;

import java.util.ArrayList;

import java.util.HashMap;

public class NamingServer {
    private HashMap<String, ServiceEntry> mapServices;

    private Boolean debug;

    public NamingServer(Boolean debug) {
        this.debug = debug;

        this.mapServices = new HashMap<String, ServiceEntry>();
    }

    public HashMap<String, ServiceEntry> getMapServices() {
        return mapServices;
    }

    public void setMapServices(HashMap<String, ServiceEntry> mapServices) {
        this.mapServices = mapServices;
    }

    public void registerService(String service, String qualifier, String target){
        if(debug) System.err.println("-> Registering server with address " + target + " and qualifier " + qualifier);

        ServerEntry server = new ServerEntry(target, qualifier);

        if(mapServices.containsKey(service)){
            mapServices.get(service).addServerEntry(server);
        }else{
            ServiceEntry serviceEntry = new ServiceEntry(service);
            serviceEntry.addServerEntry(server);
            mapServices.put(service, serviceEntry);
        }

        if(debug){
            System.out.println("Services:");
            for(String key : mapServices.keySet()){
                System.out.println(key);
                System.out.println("Provided by:");
                for(ServerEntry serverEntry : mapServices.get(key).getServerEntries()){
                    System.out.println(serverEntry.getTarget() + " " + serverEntry.getQualifier());
                }
                System.out.println();
            }
            System.out.println();
        }
    }
    
    public List<String> lookup(String service, String qualifier){
        List<String> listServers = new ArrayList<>(); //list with address of servers

        ServiceEntry serviceEntry = mapServices.get(service);

        if(serviceEntry != null){
            for(ServerEntry serverEntry : serviceEntry.getServerEntries()){

                if(!qualifier.equals("")){

                    if(serverEntry.getQualifier().equals(qualifier)){
                        if(debug) System.err.println("-> Adding server with address " + serverEntry.getTarget() + " to lookup answer");
                        listServers.add(serverEntry.getTarget());
                    }
                }else{
                    if(debug) System.err.println("-> Adding server with address " + serverEntry.getTarget() + " to lookup answer");
                    listServers.add(serverEntry.getTarget());
                }
            }
        }
        
        return listServers;
    }

    public String delete(String service, String target) throws RuntimeException{
        if(debug) System.err.println("-> Deleting server with address " + target);
        ServiceEntry serviceEntry = mapServices.get(service);

        String qualifier = new String();

        if(serviceEntry == null){
            throw new RuntimeException("Not possible to remove the server");
        }

        for(ServerEntry serverEntry : serviceEntry.getServerEntries()){
            if(serverEntry.getTarget().equals(target)){
                qualifier = serverEntry.getQualifier();
                serviceEntry.getServerEntries().remove(serverEntry);
                serverEntry = null; //nao sei se e preciso
            }
        }

        if(serviceEntry.getServerEntries().size() == 0){//if no servers provide service, service no longer exists
            mapServices.remove(service);
            serviceEntry = null;
        }

        if(debug){
            System.out.println("Services:");
            for(String key : mapServices.keySet()){
                System.out.println(key);
                System.out.println("Provided by:");
                for(ServerEntry serverEntry : mapServices.get(key).getServerEntries()){
                    System.out.println(serverEntry.getTarget() + " " + serverEntry.getQualifier());
                }
                System.out.println();
            }
            System.out.println();

        }

        return qualifier;

    }

    public Boolean debugMode(){
        return this.debug;
    }
    
    public List<String> getTargetAllServers(){
        List<String> listReturn = new ArrayList<>();

        for(ServiceEntry service : mapServices.values()){
            for(ServerEntry serverEntry : service.getServerEntries()){
                listReturn.add(serverEntry.getTarget());
            }
        }

        return listReturn;
    }
}
