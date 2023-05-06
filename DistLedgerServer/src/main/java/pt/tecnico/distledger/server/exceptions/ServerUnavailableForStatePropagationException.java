package pt.tecnico.distledger.server.exceptions;

public class ServerUnavailableForStatePropagationException extends RuntimeException{
    
    public ServerUnavailableForStatePropagationException(String address){
        super("Server with address " + address + "is currently unavailable, cannot proceed with operation");
    }

}
