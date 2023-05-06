package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

import pt.tecnico.distledger.server.exceptions.ServerAlreadyActivatedException;
import pt.tecnico.distledger.server.exceptions.ServerAlreadyDeactivatedException;
import pt.tecnico.distledger.server.exceptions.ServerDeactivatedException;
import pt.tecnico.distledger.server.exceptions.TimestampsDontMatchException;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.AccountBalanceNotSufficientException;
import pt.tecnico.distledger.server.exceptions.AccountDoesNotExistException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;


public class ServerState{

    private List<Operation> ledger;

    //map with allint[] acconts; key is account username and value is balance
    private HashMap<String, Integer> accountMap;

    private Boolean active;

    //debug flag
    private Boolean debug;

    //number of servers active
    private int numServers;

    //timestamp that represents updates in the ledger that are unstable
    private int[] replicaTS;

    //timestamp that represents stable updates in the ledger
    private int[] valueTS;

    //position to modify TS
    private int serverId;

    public ServerState(Boolean debug, int numServers, int serverId) {
        this.ledger = new ArrayList<>();
        this.accountMap  = new HashMap<String, Integer>();
        this.debug = debug;
        this.active = true;
        this.numServers = numServers;
        this.serverId = serverId;
        initTS();
        addBroker();
    }

    //add createAccount operation
    public synchronized int[] addCreateAccount(String username, List<Integer> prevTS){
        if(!getStatus()){ //if server is not active
            if (debug) System.err.println("-> Server not active, ignoring request");
            throw new ServerDeactivatedException();
        }

        incrementReplicaTS();
        int[] opTS = getOperationTS(prevTS);
        
        Operation createAccount = new CreateOp(username, opTS, prevTS, getIdentifierOtherServers());

        //if operation can be executed now, will verify conditions to send error to user; if it cant be executed now,
        //will just respond to user and save operaton in log

        /*
        if(operationHasBecomeStable(createAccount)){
            try{
                verifyCreateAccountPreConditions(username); //checks if account already exists
            }catch(AccountAlreadyExistsException e){
                throw e;
            }
        }
        */

        addToLedger(createAccount);

        if (debug) System.err.println("-> Operation create account with username '" + username + "' added to ledger");

        return opTS;
        
    }

    //add transferTo Operation
    public synchronized int[] addTransferTo(String fromAccount, String destAccount, int amount, List<Integer> prevTS){
        if(!getStatus()){ //if server is not active
            if (debug) System.err.println("-> Server not active, ignoring request");

            throw new ServerDeactivatedException();
        }

        incrementReplicaTS();
        int[] opTS = getOperationTS(prevTS);

        Operation transferTo = new TransferOp(fromAccount, destAccount, amount, opTS, prevTS, getIdentifierOtherServers()); //create transfer op

        //if operation can be executed now, will verify conditions to send error to user; if it cant be executed now,
        //will just respond to user and save operaton in log

        /*
        if(operationHasBecomeStable(transferTo)){
            try {
                verifyTransferPreConditions(fromAccount, destAccount, amount);
            } catch (AccountDoesNotExistException e){
                throw e;
            } catch (AccountBalanceNotSufficientException e){
                throw e;
            }
        }
        */

        addToLedger(transferTo);
        
        if (debug) System.err.println("-> Transfer operation added to ledger");

        return opTS;
    }




    //balance operation
    public synchronized int[] balanceVerification(String username, List<Integer> prevTS){
        if(!getStatus()){ //if server is not active
            if (debug) System.err.println("-> Server not active, ignoring request");

            throw new ServerDeactivatedException();

        }else if(!TSsizesMatch(prevTS, valueTS)){
            if(debug) System.err.println("-> TS sent by user does not have same size has TS of server");

            throw new TimestampsDontMatchException(prevTS.size(), valueTS.length);
        }
        
        while(isPrevGreaterThanNew(prevTS, valueTS)){
            System.out.println("Error: client ahead of this");
            try{
                wait();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt(); 
                System.err.println("Thread Interrupted");
            }
        }
        
        if(!usernameExists(username)){//checks if account exists
            if (debug) System.err.println("-> Account with userId '" + username + "' doesn't exist, discarding request");

            throw new AccountDoesNotExistException(username);
        }
        

        int[] toReturn = new int[numServers + 1];//first value in the array is balance to return to user; rest of array is valueTS

        toReturn[0] = getBalanceAccount(username);

        for(int i = 0; i < numServers; i++){
            toReturn[i + 1] = valueTS[i];
        }

        return toReturn; 
    }




    public void verifyCreateAccountPreConditions(String username){
        if(usernameExists(username)){ //checks if account already exists

            if (debug) System.err.println("-> Account with userId '" + username + "' already exists, discarding request");
            throw new AccountAlreadyExistsException(username);

        }
    }

    public void verifyTransferPreConditions(String fromAccount, String destAccount, int amount){
        if(!usernameExists(fromAccount)){ //if account doesnt exist
            if (debug) System.err.println("-> Account with userId '" + fromAccount + "' doesn't exist, discarding request");

            throw new AccountDoesNotExistException(fromAccount);
        }else if(!usernameExists(destAccount)){//if account doesnt exist
            if (debug) System.err.println("-> Account with userId '" + destAccount + "' doesn't exist, discarding request");

            throw new AccountDoesNotExistException(destAccount);
        }else if(getBalanceAccount(fromAccount) < amount){ //checks if account has enough balance
            if (debug) System.err.println("-> Account with userId '" + fromAccount 
                                        + "' doesn't have required balance (" + amount  + "), discarding request");

            throw new AccountBalanceNotSufficientException(fromAccount, destAccount, amount);
        }
    }


    public Boolean debugMode(){
        return debug;
    }

    public void initTS(){
        replicaTS = new int[numServers];
        valueTS = new int[numServers];

        for(int i = 0; i < numServers; i++){
            replicaTS[i] = 0;
            valueTS[i] = 0;
        }
    }

    public Boolean usernameExists(String username){
        return accountMap.containsKey(username);
    }

    public int getBalanceAccount(String username){
        return accountMap.get(username);
    }

    public synchronized void addBroker(){
        accountMap.put("broker", 1000);
    }

    public void addToLedger(Operation operation){
        ledger.add(operation);
    }

    public void addAccount(String username){
        accountMap.put(username, 0);
    }

    public void removeAccount(String username){
        accountMap.remove(username);
    }

    public void removeBalanceAccount(String username, int amount){
        accountMap.put(username, accountMap.get(username) - amount);
    }

    public void addBalanceAccount(String username, int amount){
        accountMap.put(username, accountMap.get(username) + amount);
    }

    public synchronized List<Operation> getLedger() throws ServerDeactivatedException{
        if(!getStatus()){
            if(debug) System.err.println("-> Server is not active, ignoring request");
            throw new ServerDeactivatedException();
        }
        return this.ledger;
    }

    public synchronized void setActive() throws ServerAlreadyActivatedException{
        if(this.active == false){
            if(debug) System.err.println("-> Activating server");
            this.active = true;
        }else{
            if(debug) System.err.println("-> Error: server already active");
            throw new ServerAlreadyActivatedException();
        }
    }

    public synchronized void setDeactivate() throws ServerAlreadyDeactivatedException{
        if(this.active == true){
            if(debug) System.err.println("-> Deactivating server");
            this.active = false;
        }else{
            if(debug) System.err.println("-> Error: server already deactive");
            throw new ServerAlreadyDeactivatedException();
        }
    }

    public Boolean getStatus(){
        return this.active;
    }

    public Boolean isPrevGreaterThanNew(List<Integer> prevTS, int[] newTS){//returns true if prevTS > newTS, false otherwise
        for(int i = 0; i < numServers; i++){
            if(prevTS.get(i) > newTS[i]){
                return true;
            }
        }
        return false;
    }

    public Boolean isPrevGreaterThanNew(int[] prevTS, int[] newTS){//returns true if prevTS > newTS, false otherwise
        for(int i = 0; i < numServers; i++){
            if(prevTS[i] > newTS[i]){
                return true;
            }
        }
        return false;
    }

    public Boolean TSsizesMatch(List<Integer> prevTS, int[] valueTS){
        if(prevTS.size() != valueTS.length){
            return false;
        }
        return true;
    }

    public synchronized int[] getOperationTS(List<Integer> prevTS){
        int[] temp = new int[numServers];

        for(int i = 0; i < numServers; i++){//make equal to replicaTS
            temp[i] = prevTS.get(i);
        }

        temp[serverId] = replicaTS[serverId];//increment one in the entry of this server

        if(debug){
            System.err.print("-> TS atributed to operation: ");
            printTS(temp);
            System.err.println();
        }

        return temp;
    }

    public synchronized void incrementReplicaTS(){
        replicaTS[serverId]++;
        if(debug){
            System.out.print("-> Incremented replicaTS, value is now:");
            printTS(replicaTS);
            System.out.println();  
        }
    }

    public synchronized void executeStableOperations(){
        if(debug) System.out.println("-> Checking if there are any stable operations to execute");

        sortLedger(); // make sure operations are executed in correct order

        for(Operation operation : ledger){
            if(!operation.isStable() && operationHasBecomeStable(operation)){

                if(debug){
                    System.err.print("-> Operation with ts: ");
                    printTS(operation.getOpTS());
                    System.err.println(" is now stable, executing it now");
                }

                executeOperation(operation);
            }
        }


    }

    public synchronized Boolean operationHasBecomeStable(Operation operation){
        if(isPrevGreaterThanNew(operation.getPrevTS(), valueTS)){
            return false;
        }

        return true;
    }

    public synchronized void executeOperation(Operation operation){

        if(operation.getClass().equals(CreateOp.class)){
            try {
                verifyCreateAccountPreConditions(operation.getAccount());
            } catch (AccountAlreadyExistsException e){
                operation.setStable(); //operation cannot be done, so is set stable but not executed
                return;
            }

            addAccount(operation.getAccount());

            if (debug){
                System.err.print("-> Account with username '" + operation.getAccount() + "' and id:");
                printTS(operation.getOpTS());
                System.err.println(" created successfully");
            } 

        }else{
            TransferOp transferOp = (TransferOp) operation;

            try {
                verifyTransferPreConditions(transferOp.getAccount(), transferOp.getDestAccount(), transferOp.getAmount());
            } catch (AccountDoesNotExistException e){
                operation.setStable();
                return;
            } catch (AccountBalanceNotSufficientException e){
                operation.setStable();
                return;
            }

            removeBalanceAccount(transferOp.getAccount(), transferOp.getAmount());
            addBalanceAccount(transferOp.getDestAccount(), transferOp.getAmount());
            
            if(debug){
                System.err.print("Transfer operation with id:");
                printTS(transferOp.getOpTS());
                System.err.println(" executed");
            }
        }

        updateValueTS();
        operation.setStable();
    }

    public synchronized void updateValueTS(){
        for(int i = 0; i < numServers; i++){
            if(replicaTS[i] > valueTS[i]){
                valueTS[i] = replicaTS[i];
            }
        }

        if(debug){
            System.err.print("-> Updating valueTS, is now: ");
            printTS(valueTS);
            System.err.println();
        }

        notify(); //for any balance requests that are pending, now verify if prevTS <= valueTS
    }

    public void printTS(int[] ts){
        for(int i : ts){
            System.err.print(" " + i);
        }
    }

    public void printTS(Iterable<Integer> ts){
        for(int i : ts){
            System.err.print(" " + i);
        }
    }

    public List<String> getIdentifierOtherServers(){
        List<String> returnList = new ArrayList<>();

        for(int i = 0; i < numServers; i++){
            if(i != serverId){
                char temp = (char) ((int) 'A' + i);
                String toAdd = String.valueOf(temp);
                returnList.add(toAdd);
            }
        }

        return returnList;
    }


    public List<Operation> getOperationsToGossip(String server){
        List<Operation> operationsToGossip = new ArrayList<>(); 

        sortLedger();

        for(Operation operation : ledger){
            if(operation.isServerNotGossiped(server)){
                operationsToGossip.add(operation);
                operation.removeServerNotGossiped(server);
            }
        }

        return operationsToGossip;
    }

    public void sortLedger(){
        Collections.sort(ledger);
    }

    public int[] getReplicaTS(){
        return this.replicaTS;
    }

    public String getThisServerQualifier(){
        return String.valueOf((char) ((int) 'A' + serverId));
    }

    public void gossipRequestHandler(List<Operation> operationList, List<Integer> replicaTS){
        if(!getStatus()){ //if server is not active
            if (debug) System.err.println("-> Server not active, ignoring request");
            
            throw new ServerDeactivatedException();
        }

        System.out.println("wfbeufnweoubfew");
        
        for(Operation operation : operationList){
            if(operationIsNotInLedger(operation)){
                if(debug){
                    System.err.print("-> Added to ledger operation with id: ");
                    printTS(operation.getOpTS());
                    System.err.println();
                }
                ledger.add(operation);
            }
        }


        updateReplicaTS(replicaTS);

        executeStableOperations();
    }

    public Boolean operationIsNotInLedger(Operation operation){
        int[] opTS = operation.getOpTS();
        Boolean isEqual;

        for(Operation ledgerOperation : ledger){

            isEqual = true;

            for(int i = 0; i < opTS.length; i++){
                if(opTS[i] != ledgerOperation.getOpTS()[i]){
                    isEqual = false;
                }
            }

            if(isEqual){
                return false;
            }
        }

        return true;
    }

    public void updateReplicaTS(List<Integer> receivedReplicaTS){//for gossip
        for(int i = 0; i < replicaTS.length; i++){
            if(receivedReplicaTS.get(i) > replicaTS[i]){
                replicaTS[i] = receivedReplicaTS.get(i);
            }
        }

        if(debug){
            System.err.print("-> ReplicaTS is now:");
            printTS(replicaTS);
            System.err.println();
        }
    }
    

}
