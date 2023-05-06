package pt.tecnico.distledger.server.exceptions;

public class TimestampsDontMatchException extends RuntimeException{
    
    public TimestampsDontMatchException(int clientSize, int serverSize){
        super("Received timestamp with size of " + clientSize + " servers, but it should be " + serverSize + " servers");
    }
}
