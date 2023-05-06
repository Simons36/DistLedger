package pt.tecnico.distledger.userclient;


import pt.tecnico.distledger.userclient.grpc.UserService;

public class UserClientMain {
    public static void main(String[] args) {

        final String NAMING_SERVER_TARGET = "localhost:5001";

        final int NUM_SERVERS = 3;

        Boolean debug = false;

        if(args.length == 1 && args[0].equals("-debug")){
            debug = true;
            System.out.println("Debug mode ON");
        }else{
            System.out.println("Debug mode OFF");
        }
        
        System.out.println(UserClientMain.class.getSimpleName());

        UserService userService = new UserService(NAMING_SERVER_TARGET, debug, NUM_SERVERS);

        CommandParser parser = new CommandParser(userService);
        parser.parseInput();

        userService.shutdown();
    }
}
