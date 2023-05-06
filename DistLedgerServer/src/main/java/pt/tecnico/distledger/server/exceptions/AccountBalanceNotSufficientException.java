package pt.tecnico.distledger.server.exceptions;

public class AccountBalanceNotSufficientException extends RuntimeException{
    public AccountBalanceNotSufficientException(String fromAccount, String destAccount, int amount){
        super("Account with username '" + fromAccount + "' doesn't have enough balance (" + amount +
                                    " coins) to transfer to account with username '" + destAccount + "'");
    }
}
