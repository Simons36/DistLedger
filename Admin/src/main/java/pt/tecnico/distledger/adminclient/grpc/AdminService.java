package pt.tecnico.distledger.adminclient.grpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommonServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommon.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommonServiceGrpc.NamingServerCommonServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.tecnico.distledger.adminclient.exceptions.NoServersFoundException;

public class AdminService {

    final String SERVICE_NAME = "distLedger";

    private ManagedChannel channel;

    private NamingServerCommonServiceBlockingStub stubNamingServer;

    private Boolean debug;

    private HashMap<String, List<String[]>> _serversForService;// key: service, value[0]: address of server that provides service
                                                                                    // value[1]: qualifier of that server


    public AdminService(String namingServerTarget, Boolean debug) {
        this.debug = debug;
        channel = buildChannel(namingServerTarget);
        buildNamingServerStub();
        initMapServersForService();
    }
    
    public void initMapServersForService() {
        _serversForService = new HashMap<>();
        _serversForService.put(SERVICE_NAME, new ArrayList<String[]>());

    }

    public AdminServiceBlockingStub getServerForService(String service, String qualifier) throws NoServersFoundException {
        List<String[]> serversList = _serversForService.get(service);
        if(debug) System.err.println("[DEBUG] Finding server with qualifier " + qualifier);

        if (!serversList.isEmpty()) {
            try {
                if(debug) System.err.println("[DEBUG] Trying to connect with servers in cache");

                return getServerForServiceAux(qualifier, serversList);
            } catch (Exception e) {

            }
        }

        if(debug) System.err.println("[DEBUG] Found no servers in local cache, will contact naming server");

        // no found in local table, will look for other servers

        List<String> listLookup = new ArrayList<>();
        listLookup = lookupService(service, qualifier);

        if(debug) System.err.println("[DEBUG] Lookup returned " + listLookup.size() + " servers");

        if (!listLookup.isEmpty()) {
            for (String address : listLookup) {
                serversList.add(new String[] { address, qualifier });
            }
            try {
                if(debug) System.err.println("[DEBUG] Trying to connect with servers");
                return getServerForServiceAux(qualifier, serversList);
            } catch (Exception e) {
                // error connecting, continue
            }
        }

        if(debug) System.err.println("No servers found");

        throw new NoServersFoundException(service, qualifier);
    }

    public AdminServiceBlockingStub getServerForServiceAux(String qualifier, List<String[]> serversList)
            throws RuntimeException {

        for (String[] serverInfo : serversList) {
            String target = serverInfo[0];
            String qualifierServer = serverInfo[1];

            if (qualifierServer.equals(qualifier)) {

                try {
                    if(debug) System.err.println("[DEBUG] Trying to connect with server in address " + target);

                    return connectDistLedgerServer(target);
                } catch (Exception e) {
                    // error connecting, continue
                    if(debug) System.err.println("[DEBUG] Failed to connect");
                }

                serversList.remove(serverInfo);
            }
        }



        throw new RuntimeException();

    }

    public AdminServiceBlockingStub connectDistLedgerServer(String target) throws Exception {
        try {
            return AdminServiceGrpc.newBlockingStub(buildChannel(target));
        } catch (Exception e) {
            throw e;
        }
    }

    public ManagedChannel buildChannel(String target) {
        try {
            if(debug) System.err.println("[DEBUG] Building channel for target: " + target);

            return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;//TODO: naming server not up
    }

    public void closeChannel(){
        this.channel.shutdownNow();
    }

    public void buildNamingServerStub(){
        if(debug) System.err.println("[DEBUG] Building stub for naming server");

        this.stubNamingServer = NamingServerCommonServiceGrpc.newBlockingStub(this.channel);
    }

    public AdminServiceBlockingStub buildStub(ManagedChannel channel) {
        return AdminServiceGrpc.newBlockingStub(channel);
    }

    public getLedgerStateResponse getLedgerStateService(String server){
        getLedgerStateRequest request = getLedgerStateRequest.newBuilder().build();

        try {
            if(debug) System.err.println("[DEBUG] Sending activate request to server");
            return getServerForService(SERVICE_NAME, server).getLedgerState(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (NoServersFoundException e){
            throw e;
        }
    }

    public void activateService(String server) throws RuntimeException{
        ActivateRequest request = ActivateRequest.newBuilder().build();
        try {
            if(debug) System.err.println("[DEBUG] Sending activate request to server");
            getServerForService(SERVICE_NAME, server).activate(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (NoServersFoundException e){
            throw e;
        }

    }

    public void deactivateService(String server){
        DeactivateRequest request = DeactivateRequest.newBuilder().build();

        try {
            if(debug) System.err.println("[DEBUG] Sending deactivate request to server");
            getServerForService(SERVICE_NAME, server).deactivate(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }catch(NoServersFoundException e){
            throw e;
        }

    }

    public List<String> lookupService(String service, String qualifier) {
        LookupRequest request = LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build();
        if(debug) System.err.println("[DEBUG] Lookup request sent to naming server");
        return stubNamingServer.lookup(request).getServerAddressList();

    }

    public void gossipService(String originServer, List<String> destinationServers){
        GossipRequest.Builder request = GossipRequest.newBuilder();

        if (debug){
            System.err.print("[DEBUG] Sending request to server '" + originServer + "' gossip to servers:");
            for(String server : destinationServers) System.err.print(" " + server);
            System.err.println();
        }

        for(String server : destinationServers){
            request.addQualifier(server);
        }

        try {
            getServerForService(SERVICE_NAME, originServer).gossip(request.build());
        } catch (NoServersFoundException e) {
            throw e;
        }

    }
}
