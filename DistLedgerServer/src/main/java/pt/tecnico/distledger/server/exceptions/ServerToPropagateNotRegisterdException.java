package pt.tecnico.distledger.server.exceptions;

public class ServerToPropagateNotRegisterdException extends RuntimeException{
    public ServerToPropagateNotRegisterdException(String identifier){
        super("Server with identifier '" + identifier + "' not registered in namingServer");
    }
}
