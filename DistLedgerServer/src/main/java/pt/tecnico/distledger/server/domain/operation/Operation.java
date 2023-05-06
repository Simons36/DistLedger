package pt.tecnico.distledger.server.domain.operation;

import java.util.List;

public class Operation implements Comparable<Operation>{
    private String account;

    private int[] opTS;

    private int[] prevTS;

    private Boolean stable;

    private List<String> serversNotGossiped; //list that contains all the servers that dont know this operation

    public Operation(String fromAccount, int[] opTS, List<Integer> prevTS, List<String> serversNotGossiped) {
        this.account = fromAccount;
        setOpTS(opTS);
        setPrevTS(prevTS);
        setServersNotGossiped(serversNotGossiped);
        stable = false;
    }

    public Operation(String fromAccount, List<Integer> opTS, List<Integer> prevTS, List<String> serversNotGossiped) {
        this.account = fromAccount;
        setOpTS(opTS);
        setPrevTS(prevTS);
        setServersNotGossiped(serversNotGossiped);
        stable = false;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int[] getOpTS(){
        return opTS;
    }

    public void setOpTS(int[] opTS){
        this.opTS = opTS;
    }

    public void setOpTS(List<Integer> opTS){
        this.opTS = new int[opTS.size()];

        for(int i = 0; i < opTS.size(); i++){
            this.opTS[i] = opTS.get(i);
        }
    }

    public int[] getPrevTS(){
        return prevTS;
    }

    public void setPrevTS(List<Integer> prevTS){
        this.prevTS = new int[prevTS.size()];

        for(int i = 0; i < prevTS.size(); i++){
            this.prevTS[i] = prevTS.get(i);
        }
    }

    public void setStable(){
        this.stable = true;
    }

    public Boolean isStable(){
        return this.stable;
    }

    public void setServersNotGossiped(List<String> serversNotGossiped){
        this.serversNotGossiped = serversNotGossiped;
    }

    public List<String> getServersNotGossiped(){
        return this.serversNotGossiped;
    }

    public void removeServerNotGossiped(String identifierToRemove){
        try {
            for(String identifier : getServersNotGossiped()){
                if(identifier.equals(identifierToRemove)){
                    getServersNotGossiped().remove(identifierToRemove);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public Boolean isServerNotGossiped(String identifier){
        if(getServersNotGossiped().contains(identifier)){
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(Operation otherOperation){
        Boolean hasSmallerElement = false;
        Boolean hasGreaterElement = false;

        int[] thisOpTS = opTS;
        int[] otherOpTS = otherOperation.getOpTS();

        for(int i = 0; i < opTS.length; i++){
            if(thisOpTS[i] < otherOpTS[i]){
                hasSmallerElement = true;
            }else if(thisOpTS[i] > otherOpTS[i]){
                hasGreaterElement = true;
            }
        }

        if(hasSmallerElement && !hasGreaterElement){
            return -1;
        }else if(!hasSmallerElement && hasGreaterElement){
            return 1;
        }else{
            return 0;
        }
    }

}
