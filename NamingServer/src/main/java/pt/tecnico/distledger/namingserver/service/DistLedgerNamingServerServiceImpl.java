package pt.tecnico.distledger.namingserver.service;

import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserverdistledger.DistLedgerNamingServerServiceGrpc.*;
import pt.ulisboa.tecnico.distledger.contract.namingserverdistledger.NamingServerDistLedger.*;
import io.grpc.stub.StreamObserver;

public class DistLedgerNamingServerServiceImpl extends DistLedgerNamingServerServiceImplBase{

    private NamingServer namingServer;

    public DistLedgerNamingServerServiceImpl(NamingServer namingServer){
        super();
        this.namingServer = namingServer;
    }
    
    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver){
        if(namingServer.debugMode()) System.err.println("-> Received request to register a server");

        namingServer.registerService(request.getService().toString(), request.getQualifier(), request.getTarget());
        
        RegisterResponse response = RegisterResponse.newBuilder().build();

        if(namingServer.debugMode()) System.err.println();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver){


        try {
            if(namingServer.debugMode()) System.err.println("-> Received request to delete a server");
            namingServer.delete(request.getService(), request.getTarget());
            
        } catch (RuntimeException e) {
            //TODO: exception handling
        }

        DeleteResponse response = DeleteResponse.newBuilder().build();


        if(namingServer.debugMode()) System.err.println();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
    

}