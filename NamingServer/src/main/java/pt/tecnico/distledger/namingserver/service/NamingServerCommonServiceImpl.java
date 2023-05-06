package pt.tecnico.distledger.namingserver.service;

import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommon.*;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommonServiceGrpc;
import pt.tecnico.distledger.namingserver.domain.NamingServer;
import java.util.List;

import io.grpc.stub.StreamObserver;

public class NamingServerCommonServiceImpl extends NamingServerCommonServiceGrpc.NamingServerCommonServiceImplBase{

    private NamingServer namingServer;

    public NamingServerCommonServiceImpl(NamingServer namingServer){
        super();
        this.namingServer = namingServer;
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver){
        if(namingServer.debugMode()) System.err.println("-> Received lookup request");
        List<String> listServers = namingServer.lookup(request.getService(), request.getQualifier());

        LookupResponse.Builder response = LookupResponse.newBuilder();

        for(String serverAddress: listServers){
            response.addServerAddress(serverAddress);
        }

        if(namingServer.debugMode()) System.err.println("-> Sending response to user");
        if(namingServer.debugMode()) System.err.println();

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

}
