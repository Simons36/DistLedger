package pt.tecnico.distledger.adminclient.exceptions;

public class NoServersFoundException extends RuntimeException{
    public NoServersFoundException(String service, String qualifier){
        super("No servers with qualifier '" + qualifier + "' are available for this service: " + service);
    }
}
