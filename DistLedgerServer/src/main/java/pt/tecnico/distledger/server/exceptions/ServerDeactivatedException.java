package pt.tecnico.distledger.server.exceptions;

public class ServerDeactivatedException extends RuntimeException{
    public ServerDeactivatedException(){
        super("This server is currently deactivated");
    }
}
