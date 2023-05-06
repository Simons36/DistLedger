package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final AdminService adminService;

    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            switch (cmd) {
                case ACTIVATE:
                    this.activate(line);
                    break;

                case DEACTIVATE:
                    this.deactivate(line);
                    break;

                case GET_LEDGER_STATE:
                    this.dump(line);
                    break;

                case GOSSIP:
                    this.gossip(line);
                    break;

                case HELP:
                    this.printUsage();
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    break;
            }

        }

        adminService.closeChannel();
    }

    private void activate(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];
        
        try {
            adminService.activateService(server);
            System.out.println("OK");
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    private void deactivate(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        try{
            adminService.deactivateService(server);
            System.out.println("OK");
        }catch (RuntimeException e){
            System.err.println(e.getMessage());
        }

    }

    private void dump(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        try {
            Object ledgerState = adminService.getLedgerStateService(server);
            System.out.println("OK");
            System.out.println(ledgerState);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }

    }

    @SuppressWarnings("unused")
    private void gossip(String line){
        String[] split = line.split(SPACE);

        if(split.length < 3){
            this.printUsage();
            return;
        }

        List<String> servers = new ArrayList<>();

        for(int i = 2; i < split.length; i++){
            servers.add(split[i]);
        }
        
        try {
            adminService.gossipService(split[1], servers);            
            System.out.println("OK");
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }

        
    }
    private void printUsage() {
        System.out.println("Usage:\n" +
                "- activate <server>\n" +
                "- deactivate <server>\n" +
                "- getLedgerState <server>\n" +
                "- gossip <origin server> <destination server 1 ... destination server n>\n" +
                "- exit\n");
    }

}
