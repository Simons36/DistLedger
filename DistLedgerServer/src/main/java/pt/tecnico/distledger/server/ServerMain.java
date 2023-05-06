package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.service.*;

import java.io.IOException;

public class ServerMain{
	public final static String SERVICE_NAME = "distLedger";
	
	public static void main(String[] args) throws IOException, InterruptedException{

		final String NAMINGSERVER_TARGET = "localhost:5001";//TODO: naming server address should be constant??

		final int NUM_SERVERS = 3;



        // receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

        if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}else if (args.length < 2){
            System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s server qualifier%n", ServerMain.class.getName());
            return;
        }

		final int port = Integer.parseInt(args[0]); //port to listen on
		final String this_target = "localhost:" + port;

		final String qualifier = args[1]; //qualifier of this server

		ServerState serverState;
		
        if(args.length == 3 && args[2].equals("-debug")){
			System.err.println("System debug messages ON");
			serverState  = new ServerState(true, NUM_SERVERS, (int) qualifier.charAt(0) - 'A'); 
        }else{
			serverState = new ServerState(false, NUM_SERVERS, (int) qualifier.charAt(0) - 'A');
		}

		//creating naming server channel for distLedger -> namingServer communication
		DistLedgerNamingServerService namingServerService = new DistLedgerNamingServerService(NAMINGSERVER_TARGET);																														
		
		final BindableService implCrossServer = new DistLedgerCrossServerServiceImpl(serverState, namingServerService);
		final BindableService implUser = new UserServiceImpl(serverState);
		final BindableService implAdmin = new AdminServiceImpl(serverState, (DistLedgerCrossServerServiceImpl) implCrossServer);
		
		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port)
									 .addService(implUser)
									 .addService(implAdmin)
									 .addService(implCrossServer)
									 .build();
									 
		// Start the server
		try{
			server.start();
			if(serverState.debugMode()) System.err.println("Server started");
			if(serverState.debugMode()) System.err.println();
			// Server threads are running in the background.
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
									 
		try {
			namingServerService.registerService(SERVICE_NAME, qualifier, this_target);
			
		} catch (RuntimeException e) {
			System.out.println("Error registering in naming server: " + e.getMessage());
			return;
		}



        if(serverState.debugMode()) System.err.printf("Created server, listening on port %d%n", port);


		System.out.println("Press enter to shutdown");
		System.in.read();
		if(serverState.debugMode()) System.err.println("Server closing");
		namingServerService.deleteService(SERVICE_NAME, this_target);
		server.shutdown();

    }

}

