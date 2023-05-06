package pt.tecnico.distledger.server.exceptions;

public class ServerAlreadyDeactivatedException extends RuntimeException{
    public ServerAlreadyDeactivatedException(){
        super("Server is already deactive");
    }
}
