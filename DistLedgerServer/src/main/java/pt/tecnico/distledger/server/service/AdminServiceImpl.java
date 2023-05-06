package pt.tecnico.distledger.server.service;


import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.exceptions.ServerAlreadyActivatedException;
import pt.tecnico.distledger.server.exceptions.ServerAlreadyDeactivatedException;
import pt.tecnico.distledger.server.exceptions.ServerDeactivatedException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableForStatePropagationException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.UNAVAILABLE;

import java.util.List;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private ServerState _state;

    private DistLedgerCrossServerServiceImpl crossServerService;

    public AdminServiceImpl(ServerState state, DistLedgerCrossServerServiceImpl crossServerService) {
        super();
        _state = state;
        this.crossServerService = crossServerService;
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver){
        if(_state.debugMode()) System.err.println("-> Received LedgerState request from admin");

        List<pt.tecnico.distledger.server.domain.operation.Operation> ledger;

        try{
            ledger = _state.getLedger();
        }catch (ServerDeactivatedException e) {
            if(_state.debugMode()) System.err.println("-> Sending error to user");

            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());

            if(_state.debugMode()) System.err.println();

            return;
        }

        
        LedgerState.Builder ledgerState = ServiceCommon.parseLedgerState(ledger);

        getLedgerStateResponse response = getLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build();

        if(_state.debugMode()) System.err.println("-> Sending LedgerState response to admin");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if(_state.debugMode()) System.err.println();
        
    }


    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver){
        if(_state.debugMode()) System.err.println("-> Received request to activate server from admin");

        try{
            _state.setActive(); //set server active
        }catch(ServerAlreadyActivatedException e){
            if(_state.debugMode()) System.err.println("-> Sending error to user");
            responseObserver.onError(FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
            if(_state.debugMode()) System.err.println();

            return;
        }

        ActivateResponse response = ActivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if(_state.debugMode()) System.err.println();
    }


    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver){
        if(_state.debugMode()) System.err.println("-> Received request to deactivate server from admin");
        
        try {
            _state.setDeactivate(); //set server deactive
            
        } catch (ServerAlreadyDeactivatedException e) {
            if(_state.debugMode()) System.err.println("-> Sending error to user");
            responseObserver.onError(FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
            if(_state.debugMode()) System.err.println();

            return;
        }

        DeactivateResponse response = DeactivateResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if(_state.debugMode()) System.err.println();
    }


    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver){
        List<String> serversToGossip = request.getQualifierList();

        if(_state.debugMode()){
            System.err.print("-> Received request to gossip to servers:");
            for(String server : serversToGossip){
                System.err.print(" " + server);
            }
            System.err.println();
        } 

        for(String identifier : request.getQualifierList()){
            try {
                List<Operation> operations = _state.getOperationsToGossip(identifier);
                crossServerService.propagateStateService(operations, identifier);
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            } catch (ServerUnavailableForStatePropagationException e){
                responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
                return;
            }
        }

        responseObserver.onNext(GossipResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
