package pt.tecnico.distledger.server.domain.operation;

import java.util.List;

public class TransferOp extends Operation {
    private String destAccount;
    private int amount;

    public TransferOp(String fromAccount, String destAccount, int amount, int[] opTS, List<Integer> prevTS, List<String> serversNotGossiped) {
        super(fromAccount, opTS, prevTS, serversNotGossiped);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    public TransferOp(String fromAccount, String destAccount, int amount, List<Integer> opTS, List<Integer> prevTS, List<String> serversNotGossiped) {
        super(fromAccount, opTS, prevTS, serversNotGossiped);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    public String getDestAccount() {
        return destAccount;
    }

    public void setDestAccount(String destAccount) {
        this.destAccount = destAccount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

}
