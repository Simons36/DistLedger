package pt.tecnico.distledger.server.service;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.*;


import static io.grpc.Status.NOT_FOUND;
import static io.grpc.Status.UNAVAILABLE;
import static io.grpc.Status.UNKNOWN;
import static io.grpc.Status.INVALID_ARGUMENT;

import io.grpc.StatusRuntimeException;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private ServerState _state;

    public UserServiceImpl(ServerState state) {
        super();
        _state = state;
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver){
        int[] opTS;

        try {

            if(_state.debugMode()){
                System.err.print("-> Received createAccount request with userid: " + request.getUserId() + " and client timestamp: ");
                _state.printTS(request.getPrevTSList());
                System.err.println();
            } 


            opTS = _state.addCreateAccount(request.getUserId(), request.getPrevTSList());
        }catch(Exception e){
            if(_state.debugMode()) System.err.println("-> Sending error to user");

            responseObserver.onError(exceptionHandler(e));

            if(_state.debugMode()) System.err.println();
            return;
        }

        CreateAccountResponse.Builder response = CreateAccountResponse.newBuilder();

        for(int i : opTS){
            response.addTS(i);
        }

        if(_state.debugMode()) System.err.println("-> Responding to user with operation timestamp");

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();

        _state.executeStableOperations(); //after sending response to user, will check if it can execute any operations in ledger

        if(_state.debugMode()) System.err.println();
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        int[] returned;

        if(_state.debugMode()){
            System.err.print("-> Received balance request for userid: " + request.getUserId() + " and client timestamp: ");
            _state.printTS(request.getPrevTSList());
            System.err.println();
        } 

        try {
            returned = _state.balanceVerification(request.getUserId(), request.getPrevTSList());
        }catch(Exception e){
            if(_state.debugMode()) System.err.println("-> Sending error to user");

            responseObserver.onError(exceptionHandler(e));

            if(_state.debugMode()) System.err.println();
            return;
        }

        BalanceResponse.Builder response = BalanceResponse.newBuilder().setValue(returned[0]);
        for(int i = 1; i < returned.length; i++){
            response.addValueTS(returned[i]);
        }

        if(_state.debugMode()) System.err.println("-> Sending user balance of account (" + returned[0] + " coins)");



        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        if(_state.debugMode()) System.err.println();
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        if(_state.debugMode()){
            System.err.print("-> Received transfer request, " + request.getAccountFrom()+ " to "
                                                              + request.getAccountTo() + " "
                                                              + request.getAmount() + " coins and client timestamp: ");
                                                              _state.printTS(request.getPrevTSList());
            System.err.println();

        } 

        int opTS[];

        try {

            opTS = _state.addTransferTo(request.getAccountFrom(), request.getAccountTo(), request.getAmount(), request.getPrevTSList());
        }catch(Exception e){
            if(_state.debugMode()) System.err.println("-> Sending error to user");

            responseObserver.onError(exceptionHandler(e));

            if(_state.debugMode()) System.err.println();
            return;
        }

        TransferToResponse.Builder response = TransferToResponse.newBuilder();

        for(int i : opTS){
            response.addTS(i);
        }

        if(_state.debugMode()) System.err.println("-> Responding to user with operation timestamp");

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();

        _state.executeStableOperations(); //after sending response to user, will check if it can execute any operations in ledger

        if(_state.debugMode()) System.err.println();
    }

    public StatusRuntimeException exceptionHandler(Exception e){
        if(e.getClass().equals(ServerDeactivatedException.class)){
            return UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException();
        }else if(e.getClass().equals(TimestampsDontMatchException.class)){
            return INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
        }else if(e.getClass().equals(AccountDoesNotExistException.class)){
            return NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
        }else{
            return UNKNOWN.withDescription(e.getMessage()).asRuntimeException();
        }
    }
}