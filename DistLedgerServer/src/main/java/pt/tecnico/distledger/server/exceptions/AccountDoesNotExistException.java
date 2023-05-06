package pt.tecnico.distledger.server.exceptions;

public class AccountDoesNotExistException extends RuntimeException{
    public AccountDoesNotExistException(String username){
        super("Account with name '" + username + "' doesn't exist");
    }
}
