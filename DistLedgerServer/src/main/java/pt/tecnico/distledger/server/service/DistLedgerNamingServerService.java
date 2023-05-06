package pt.tecnico.distledger.server.service;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.namingserverdistledger.DistLedgerNamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserverdistledger.NamingServerDistLedger.*;


public class DistLedgerNamingServerService{

    private ManagedChannel channel;

    private DistLedgerNamingServerServiceGrpc.DistLedgerNamingServerServiceBlockingStub stub;

    public DistLedgerNamingServerService(String target){
        super();
        buildChannel(target);
        buildStub();
    }

    public void buildChannel(String target){
        try {
            channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void buildStub(){
        stub = DistLedgerNamingServerServiceGrpc.newBlockingStub(channel);
    }

    public void closeChannel(){

    }

    public void registerService(String service, String qualifier, String target) throws RuntimeException{
        RegisterRequest request = RegisterRequest.newBuilder().setService(service)
                                                              .setQualifier(qualifier)
                                                              .setTarget(target)
                                                              .build();

        try {
            stub.register(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }                                            
    }

    public List<String> lookupService(String service, String qualifier){
        return ServiceCommon.lookupService(service, qualifier, channel);
    }

    public void deleteService(String service, String target){
        DeleteRequest request = DeleteRequest.newBuilder().setService(service)
                                                          .setTarget(target)
                                                          .build();
                                                          
        stub.delete(request);
        channel.shutdownNow();
    }

    
    
    
}
