package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.tecnico.distledger.namingserver.service.NamingServerCommonServiceImpl;
import pt.tecnico.distledger.namingserver.service.DistLedgerNamingServerServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class NamingServerMain {

    public static void main(String[] args) {

        System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

        if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", NamingServerMain.class.getName());
			return;
		}

		Boolean debug = false;

        if(args.length == 2 && args[1].equals("-debug")){
            debug = true;
            System.out.println("Debug mode ON");
        }else{
            System.out.println("Debug mode OFF");
        }


		final int port = Integer.parseInt(args[0]);  //port to listen for connections

		NamingServer namingServer = new NamingServer(debug);

		DistLedgerNamingServerServiceImpl implDistServer = new DistLedgerNamingServerServiceImpl(namingServer);
		NamingServerCommonServiceImpl implCommon = new NamingServerCommonServiceImpl(namingServer);

		Server server = ServerBuilder.forPort(port).addService(implDistServer).addService(implCommon).build();

		try{
            server.start();
            // Server threads are running in the background.
		} catch (IOException e){
			System.err.println(e.getMessage());
		}

		
		try {
			server.awaitTermination();
		} catch (InterruptedException e){
			e.getLocalizedMessage();
		}

    }

}
