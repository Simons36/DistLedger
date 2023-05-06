package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//import pt.tecnico.distledger.userclient.grpc.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommon.*;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommonServiceGrpc;
import pt.tecnico.distledger.userclient.exceptions.NoServersFoundException;

public class UserService {

    final String SERVICE_NAME = "distLedger";

    private ManagedChannel _channelNamingServer; //

    private NamingServerCommonServiceGrpc.NamingServerCommonServiceBlockingStub _stubNamingServer;

    private HashMap<String, List<String[]>> _serversForService;// key: service, value[0]: address of server that provides service
                                                                                    // value[1]: qualifier of that server

    private Boolean debug;

    private int[] prevTS;

    public UserService(String namingServerTarget, Boolean debug, int numServers) {
        this.debug = debug;
        _channelNamingServer = buildChannel(namingServerTarget);
        buildNamingServerStub();

        initMapServersForService();

        initPrevTS(numServers);
    }

    public void initMapServersForService() {
        _serversForService = new HashMap<>();
        _serversForService.put(SERVICE_NAME, new ArrayList<String[]>());
        
    }

    public void initPrevTS(int numServers){
        prevTS = new int[numServers];
        for(int i : prevTS){
            i = 0;
        }
    }

    public UserServiceGrpc.UserServiceBlockingStub getServerForService(String service, String qualifier) throws NoServersFoundException {
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

    public UserServiceGrpc.UserServiceBlockingStub getServerForServiceAux(String qualifier, List<String[]> serversList)
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

    public ManagedChannel buildChannel(String target) {
        try {
            if(debug) System.err.println("[DEBUG] Building channel for target: " + target);

            return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        } catch (Exception e) {// error connecting to target
            System.out.println(e.getMessage());
            return null;
        }

    }

    public void buildNamingServerStub() {
        if(debug) System.err.println("[DEBUG] Building stub for naming server");
        _stubNamingServer = NamingServerCommonServiceGrpc.newBlockingStub(_channelNamingServer);
    }

    public UserServiceGrpc.UserServiceBlockingStub connectDistLedgerServer(String target) throws Exception {
        try {
            return UserServiceGrpc.newBlockingStub(buildChannel(target));
        } catch (Exception e) {
            throw e;
        }
    }

    public void closeChannel(ManagedChannel channel) {
        channel.shutdownNow();
    }

    public void shutdown() {
        closeChannel(_channelNamingServer);
    }

    public void createAccountService(String qualifier, String username) throws RuntimeException {
        CreateAccountRequest.Builder createAccountRequest = CreateAccountRequest.newBuilder().setUserId(username);

        for(int i : prevTS){
            createAccountRequest.addPrevTS(i);
        }

        try {
            if(debug) System.err.println("[DEBUG] Sending createAccount request");

            List<Integer> newTS = getServerForService(SERVICE_NAME, qualifier).createAccount(createAccountRequest.build()).getTSList();

            if(debug){
                System.err.print("[DEBUG] Received timestamp with values:");
                for(int i : newTS){
                    System.err.print(" " + i);
                }
                System.err.println();
            }

            setPrevTS(newTS);

        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (NoServersFoundException e) {
            throw e;
        }
    }

    public int balanceService(String qualifier, String username) throws RuntimeException {
        BalanceRequest.Builder balanceRequest = BalanceRequest.newBuilder().setUserId(username);

        for(int i : prevTS){
            balanceRequest.addPrevTS(i);
        }

        try {
            if(debug) System.err.println("[DEBUG] Sending balance request");

            BalanceResponse response = getServerForService(SERVICE_NAME, qualifier).balance(balanceRequest.build());

            int value = response.getValue();
            List<Integer> newTS = response.getValueTSList();

            if(debug){
                System.err.print("[DEBUG] Received timestamp with values:");
                for(int i : newTS){
                    System.err.print(" " + i);
                }
                System.err.println();
            }

            setPrevTS(newTS);

            return value;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (NoServersFoundException e){
            throw e;
        }
    }

    public void transferToService(String qualifier, String accountFrom, String accountTo, int amount)
            throws RuntimeException {
        TransferToRequest.Builder transferToRequest = TransferToRequest.newBuilder()
                .setAccountFrom(accountFrom)
                .setAccountTo(accountTo)
                .setAmount(amount);

        for(int i : prevTS){
            transferToRequest.addPrevTS(i);
        }

        try {
            if(debug) System.err.println("[DEBUG] Sending transferTo request");

            List<Integer> newTS = getServerForService(SERVICE_NAME, qualifier).transferTo(transferToRequest.build()).getTSList();

            if(debug){
                System.err.print("[DEBUG] Received timestamp with values:");
                for(int i : newTS){
                    System.err.print(" " + i);
                }
                System.err.println();
            }

            setPrevTS(newTS);

        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (NoServersFoundException e){
            throw e;
        }
    }

    public List<String> lookupService(String service, String qualifier) {
        LookupRequest request = LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build();
        if(debug) System.err.println("[DEBUG] Lookup request sent to naming server");
        return _stubNamingServer.lookup(request).getServerAddressList();

    }

    public void setPrevTS(List<Integer> newTS){
        for(int i = 0; i < prevTS.length; i++){
            prevTS[i] = newTS.get(i);
        }
    }

}
