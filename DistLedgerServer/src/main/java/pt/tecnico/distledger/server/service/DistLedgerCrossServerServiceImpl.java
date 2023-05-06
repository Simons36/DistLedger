package pt.tecnico.distledger.server.service;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.tecnico.distledger.server.domain.operation.Operation;

import pt.tecnico.distledger.server.ServerMain;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.grpc.stub.StreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import static io.grpc.Status.UNKNOWN;
import static io.grpc.Status.UNAVAILABLE;

public class DistLedgerCrossServerServiceImpl extends DistLedgerCrossServerServiceImplBase{

    private ServerState serverState;

    private DistLedgerNamingServerService namingServer;

    private HashMap<String, ManagedChannel> serverChannelCache; //key: qualifier, value: address

    public DistLedgerCrossServerServiceImpl(ServerState serverState, DistLedgerNamingServerService namingServer){
        super();
        this.serverState = serverState;
        this.namingServer = namingServer;
        this.serverChannelCache = new HashMap<>();
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver){
        if(serverState.debugMode()){
            System.err.println("-> Received state propagation");
        }

        List<pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation> protoOperationList = request.getState().getLedgerList();

        List<Operation> operationList = new ArrayList<>();

        if(serverState.debugMode()) System.err.println("-> Received operations with id:");
        for(pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation protoOperation : protoOperationList){
            Operation operation = ServiceCommon.parseProtoOperationToOperation(protoOperation);
            operation.removeServerNotGossiped(serverState.getThisServerQualifier());
            operationList.add(operation);
            if (serverState.debugMode()){
                serverState.printTS(operation.getOpTS());
                System.err.println();
            } 
        }
        
        System.out.println(operationList.size());
        try {
            serverState.gossipRequestHandler(operationList, request.getReplicaTSList());
        }catch(ServerDeactivatedException e){
            if(serverState.debugMode()){
                System.err.println("-> Reporting error to user");
            }

            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
            return;
        }catch (Exception e) {
            if(serverState.debugMode()){
                System.err.println("-> Reporting error to user " + e.getMessage());
            }

            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        responseObserver.onNext(PropagateStateResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void propagateStateService(List<Operation> operations, String identifier){
        if(serverState.debugMode()) System.err.println("-> Received request to propagate state to server: " + identifier);

        PropagateStateRequest.Builder request = PropagateStateRequest.newBuilder();
        LedgerState.Builder ledgerList = LedgerState.newBuilder();

        if(serverState.debugMode()) System.err.println("-> Propagating operations with id: ");
        for(Operation operation : operations){
            ledgerList.addLedger(ServiceCommon.parseOperationToProtoOperation(operation));
            if (serverState.debugMode()){
                serverState.printTS(operation.getOpTS());
                System.err.println();
            } 
        }

        request.setState(ledgerList.build());

        int[] replicaTS = serverState.getReplicaTS();

        for(int i : replicaTS){
            request.addReplicaTS(i);
        }

        if(serverState.debugMode()) System.err.print("-> Propagating replicaTS:");
        if(serverState.debugMode()) serverState.printTS(serverState.getReplicaTS());
        if(serverState.debugMode()) System.err.println();

        try {
            getStub(identifier).propagateState(request.build());
        } catch (ServerUnavailableForStatePropagationException e) {
            throw e;
        }catch (StatusRuntimeException e) {
            if(serverState.debugMode()){
                System.err.println("-> Error in gossip: " + e.getMessage());
            }
            throw e;
        }

    }
    

    public ManagedChannel buildChannel(String target){
        try {
            return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        } catch (Exception e) {
            throw e;
        }
    }

    public DistLedgerCrossServerServiceBlockingStub buildStub(ManagedChannel channel){
        try{
            return DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        }catch(Exception e){
            throw e;
        }
    }

    public ManagedChannel createChannelForOtherServers(String identifier) throws ServerUnavailableForStatePropagationException{
        

        List<String> addressList = namingServer.lookupService(ServerMain.SERVICE_NAME, identifier);

        if(addressList.isEmpty()){
            if(serverState.debugMode()) System.err.println("-> Error: secondary server not found in naming server");
            throw new ServerToPropagateNotRegisterdException(identifier);
        }

        String address = addressList.get(0); //when qualifier is specified, lookup only returns one address
        if(serverState.debugMode()) System.err.println("-> Found address for server: " + address);

        try{
            return buildChannel(address);
        }catch(Exception e){
            if(serverState.debugMode()) System.err.println("-> Error propagating to server: " + e.getMessage());

            throw new ServerUnavailableForStatePropagationException(address);
        }
    }

    public DistLedgerCrossServerServiceBlockingStub getStub(String identifier){
        if(serverState.debugMode()) System.err.print("-> Checking channel cache for server with identifier: " + identifier);

        if(serverChannelCache.containsKey(identifier)){
            try {
                if(serverState.debugMode()) System.err.println("-> Channel for identifier found");

                return buildStub(serverChannelCache.get(identifier));
            } catch (Exception e) {
                if(serverState.debugMode()) System.err.println("-> Error building stub, will do lookup");

                serverChannelCache.remove(identifier);
            }
        }else{
            if(serverState.debugMode()) System.err.print("-> Identifier not found in cache, will do lookup");
        }

        try {
            ManagedChannel channel = createChannelForOtherServers(identifier);
            serverChannelCache.put(identifier, channel);
            return buildStub(channel);
        } catch (ServerUnavailableForStatePropagationException e) {
            throw e;
        }
    }

    public void closeChannel(ManagedChannel channel){
        channel.shutdownNow();
    }
    
}
