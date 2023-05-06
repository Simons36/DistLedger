package pt.tecnico.distledger.server.service;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommon.*;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommonServiceGrpc.NamingServerCommonServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.namingservercommon.NamingServerCommonServiceGrpc;

import java.util.List;

import io.grpc.ManagedChannel;

/**
 * Functions shared by classes in service
 */
public class ServiceCommon extends NamingServerCommonServiceImplBase{

    /**
     * Takes a list<Operation> and returns a LedgerState.Builder
     */
    public static LedgerState.Builder parseLedgerState(List<pt.tecnico.distledger.server.domain.operation.Operation> ledger){
        LedgerState.Builder ledgerState = LedgerState.newBuilder();
        
        for(pt.tecnico.distledger.server.domain.operation.Operation operation : ledger){
            ledgerState.addLedger(parseOperationToProtoOperation(operation));
        }

        return ledgerState;
    }

    public static List<String> lookupService(String service,String qualifier, ManagedChannel channel){
        LookupRequest.Builder request = LookupRequest.newBuilder().setService(service);

        if(qualifier != null){
            request.setQualifier(qualifier);
        }

        return NamingServerCommonServiceGrpc.newBlockingStub(channel).lookup(request.build()).getServerAddressList();

    }

    public static Operation parseOperationToProtoOperation(pt.tecnico.distledger.server.domain.operation.Operation operation){
        Operation.Builder protoOperation = Operation.newBuilder();
        protoOperation.setUserId(operation.getAccount()); //get userId of op

        if(operation.getClass().equals(pt.tecnico.distledger.server.domain.operation.TransferOp.class)){ //transferOp
            protoOperation.setType(OperationType.OP_TRANSFER_TO);
            protoOperation.setDestUserId(((TransferOp) operation).getDestAccount());
            protoOperation.setAmount(((TransferOp) operation).getAmount());    
        }else{//createAccount
            protoOperation.setType(OperationType.OP_CREATE_ACCOUNT);
        }

        for(int i = 0; i < operation.getPrevTS().length; i++){//add timestamps
            protoOperation.addPrevTS(operation.getPrevTS()[i]);
            protoOperation.addTS(operation.getOpTS()[i]);
        }

        for(String server : operation.getServersNotGossiped()){//add servers not gossiped to
            protoOperation.addServersNotGossiped(server);
        }


        return protoOperation.build();
    }

    public static pt.tecnico.distledger.server.domain.operation.Operation parseProtoOperationToOperation(Operation protoOperation){
        if(protoOperation.getType().equals(OperationType.OP_CREATE_ACCOUNT)){
            return new CreateOp(protoOperation.getUserId(), protoOperation.getTSList(), protoOperation.getPrevTSList(), protoOperation.getServersNotGossipedList());
        }else if(protoOperation.getType().equals(OperationType.OP_TRANSFER_TO)){
            return new TransferOp(protoOperation.getUserId(), protoOperation.getDestUserId(), protoOperation.getAmount(), protoOperation.getTSList(), protoOperation.getPrevTSList(), protoOperation.getServersNotGossipedList());
        }else{
            throw new RuntimeException("Error in comunication"); //operation type is unspecified
        }
    }
}
