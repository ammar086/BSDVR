package app;

import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.FileNotFoundException;

public class Topology {

    public static Integer num_nodes = -1;
    public static Integer id_min = 11;
    public static Integer id_max = 210;
    public static Integer port_min = 1234;
    public static Integer port_max = 1434;
    public static String ws_path = System.getProperty("user.dir")+"/bin/";
    public static void main(String[] args){
        System.out.println("initializing topology ...");
        try{
            if(args.length == 1){
                String f_path = System.getProperty("user.dir")+"/src/app/"+ args[0]; 
                ArrayList<ArrayList<Integer> > links = readfile(f_path);
                ArrayList<String[]> commands = generateInstances(num_nodes, links);
                for(String[] c:commands){
                    Process p = Runtime.getRuntime().exec(c);
                    p.waitFor();
                }
            } else {
                System.out.println("Expected: <filename e.g t1.txt>");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public static ArrayList<ArrayList<Integer>> readfile(String path) {
        ArrayList<ArrayList<Integer> > links = new ArrayList<ArrayList<Integer>>();
        try{
            File f = new File(path);
            Scanner f_reader = new Scanner(f);
            num_nodes = Integer.parseInt(f_reader.nextLine().split(" = ")[1]);
            while (f_reader.hasNextLine()) {
                String data = f_reader.nextLine();
                if(!data.contains("cost")){
                    ArrayList<Integer> vec= new ArrayList<Integer>();
                    String[] vectors = data.split(", ");
                    for(String v:vectors){
                        vec.add(Integer.parseInt(v));
                    }
                    links.add(vec);
                }
            }
            f_reader.close();
        } catch (FileNotFoundException e){
            System.out.println(e.toString());
        }
        return links;
    };
    public static ArrayList<String[]> generateInstances(Integer num_nodes, ArrayList<ArrayList<Integer>> links){
        ArrayList<Integer> ids = new ArrayList<Integer>(num_nodes);
        ArrayList<Integer> ports = new ArrayList<Integer>(num_nodes);
        ArrayList<String[]> commands = new ArrayList<String[]>();
        if(num_nodes <= 88){
            for(int i=0; i < num_nodes; i++){
                ids.add(id_min + i);
                ports.add(port_min + i);
            }
            for(int i= 0; i < num_nodes; i++){
                ArrayList<ArrayList<Integer>> nlinks = new ArrayList<ArrayList<Integer>>();
                for(ArrayList<Integer> link:links){
                    if(link.get(0).equals(ids.get(i))){
                        nlinks.add(link);
                    }
                }
                String nvec = nlinks.toString().replace("[", "").replaceAll("]", "").replace(",", "");
                String[] c = { "osascript", "-e", "tell application \"Terminal\" to do script \"cd "+ws_path+" && java app.App "+
                                Integer.toString(ports.get(i))+" "+Integer.toString(ids.get(i))+" "+nvec+"\"" };
                commands.add(c);
            }
        }else{
            System.out.println("Error: Number of nodes exceed allocation space");
            return commands;
        }
        return commands;
    };
}