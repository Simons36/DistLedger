package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {
    public static void main(String[] args) {

        final String NAMING_SERVER_TARGET = "localhost:5001";

        Boolean debug = false;

        if(args.length == 1 && args[0].equals("-debug")){
            debug = true;
            System.out.println("Debug mode ON");
        }else{
            System.out.println("Debug mode OFF");
        }

        System.out.println(AdminClientMain.class.getSimpleName());

        AdminService adminService = new AdminService(NAMING_SERVER_TARGET, debug);

        CommandParser parser = new CommandParser(adminService);
        parser.parseInput();

        adminService.closeChannel();
    }
}
