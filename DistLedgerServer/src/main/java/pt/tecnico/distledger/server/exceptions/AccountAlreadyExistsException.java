package pt.tecnico.distledger.server.exceptions;

public class AccountAlreadyExistsException extends RuntimeException{
    public AccountAlreadyExistsException(String username){
        super("Account with name '" + username + "' already exists");
    }
}
