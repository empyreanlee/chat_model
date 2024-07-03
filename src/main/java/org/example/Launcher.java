package org.example;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.rmi.MarshalledObject;
import java.util.Scanner;

public class Launcher {
    public static void main(String[] args) throws Exception {
        String ipPort = args[0];
        String nodeName = args[1];
        String rootName = null;

        if (args.length > 2)
            rootName = args[2];

        DirContext ldapContext = Utilities.createLdapContext(ipPort);

        try {
            ldapContext.lookup("cn=" + nodeName + ",ou= nodes");
            System.err.println("Error: Node (" + nodeName + ")already exists");
        } catch (NamingException ignored){
        }
        Node node = new Node(ipPort, nodeName, rootName);
        ldapContext.bind("cn=" + nodeName+",ou=nodes", new MarshalledObject<>(node), null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try{
                ldapContext.unbind("cn= " + nodeName + ",ou=nodes");
                System.out.println(nodeName + "Unbound");
                System.out.flush();
            } catch (Exception e){
                e.printStackTrace(System.err);
            }}));
        System.out.println(node.log());
        try (Scanner scanner = new Scanner(System.in)) {
            String[] command;
            while (true) {
                command = scanner.nextLine().trim().split(" ");
                switch (command[0]) {
                    case "log":
                        System.out.println(node.log());
                        break;
                    case "parent":
                        node.connectParent(command[1]);
                        break;
                    case "Registry":
                        NamingEnumeration<NameClassPair> entries = ldapContext.list("ou=nodes");
                        while (entries.hasMore()) {
                            NameClassPair entry = entries.next();
                            System.out.println("Registered node " + entry.getName());
                        }
                        break;
                    case "server":
                        node.connectServer(command[1], Integer.parseInt(command[2]));

                }
            }
        } catch (Exception e){
            e.printStackTrace(System.err);

        }
    }
}
