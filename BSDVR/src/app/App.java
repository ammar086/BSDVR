package app;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {

    public static Router node;
    public static Print print;
    public static String[] links;

    public static void printInstructions() {
        System.out.println("\nr: print router details\n" + "l: print link table\n" + "d: print distance vector table\n"    
        + "f: print forwarding table\n" + "b: print buffer\n" + "t: print file transmissions\n" + "i: print instuctions\n"  
        + "c: establish connection to router\n" + "s: send data to neighbor\n" + "x: disconnect from all neighbors\n" 
        + "z: reconnect with all neighbors\n" + "e: exit the router emulation");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("\n***Binary State Distance Vector Routing Protocol***\n");
        if (args.length >= 2) {
            node = new Router(args[0], args[1]);
            print = new Print(node);
            links = Arrays.copyOfRange(args, 2, args.length);
        } else {
            System.out.println("Expected: <port e.g 8080> <name e.g Adam>");
            System.exit(0);
        }
        printInstructions();
        String[] inputs;
        Scanner in = new Scanner(System.in);
        TimeUnit.SECONDS.sleep(1);
        node.restoreLinks(links);
        while (true) {
            System.out.printf("\n" + Router.translateID(node.getID()) + ":> ");
            String input = in.nextLine();
            if (input.equals("c")) {
                System.out.println("\nEnter host addr, port and link cost respectively:\n");
                inputs = in.nextLine().split(" ");
                while (inputs.length != 3) {
                    System.out.println("\nExpected: <addr e.g localhost> <port e.g 8080> <cost e.g 4>\n");
                    inputs = in.nextLine().split(" ");
                }
                node.addLink(inputs[0], Integer.parseInt(inputs[1]), Integer.parseInt(inputs[2]));
            } else if (input.equals("r")) {
                print.printRouter();
            } else if(input.equals("l")){
                print.printLT();
            } else if (input.equals("d")) {
                print.printDVT(); 
            } else if(input.equals("f")) {
                print.printFT();
            } else if(input.equals("b")) {
                print.printMBuffer();
                //node.printDigests();
            } else if(input.equals("t")) {
                print.printFBuffer();
            } else if (input.equals("i")) {
                printInstructions();
            } else if (input.equals("s")) {
                String [] dest;
                System.out.println("\nEnter file name and destination(s):\n");
                inputs = in.nextLine().split(" ");
                while (inputs.length <= 1){
                    System.out.println("\nExpected: <file name e.g apple.jpeg> <destionation(s) e.g 11 or 11 12 13>\n");
                    inputs = in.nextLine().split(" ");
                }
                dest = Arrays.copyOfRange(inputs, 1, inputs.length);
                node.transmitFile(inputs[0], dest);
            } else if (input.equals("x")) {
                System.out.println("\nEnter neighbor id(s) or \"ALL\" for disconnecting selective link(s):\n");
                inputs = in.nextLine().split(" ");
                while(inputs.length < 1){
                    System.out.println("\nExpected: <neigbors(s) e.g 11 or 11 12 13 or ALL>\n");
                    inputs = in.nextLine().split(" ");
                }
                node.disconnect(inputs);
            } else if (input.equals("z")) {
                System.out.println("\nEnter neighbor id(s) or \"ALL\" for reconnecting selective link(s):\n");
                inputs = in.nextLine().split(" ");
                while(inputs.length < 1){
                    System.out.println("\nExpected: <neigbors(s) e.g 11 or 11 12 13 or ALL>\n");
                    inputs = in.nextLine().split(" ");
                }
                node.reconnect(inputs);
            } else if (input.equals("p")){
                System.out.println("***SENT***");
                node.getDCstats().printStats(0);
                System.out.println("***RECIEVED***");
                node.getCstats().printStats(0);
            } else if (input.equals("m")){
                System.out.println("***SENT***");
                node.getDCstats().printMessages(0);
                System.out.println("***RECIEVED***");
                node.getCstats().printMessages(0);
            } else if (input.equals("e")) {
                node.terminateRouter();
                break;
            } else {
                System.out.println("Error: Invalid command provided, enter 'i' for instructions");
            }
        }
        in.close();
        System.out.println("Exiting router emulation...");
        System.exit(0);
    }
}