package pt.tecnico.distledger.server.exceptions;

public class ServerAlreadyActivatedException extends RuntimeException{
    public ServerAlreadyActivatedException(){
        super("Server is already active");
    }
}
