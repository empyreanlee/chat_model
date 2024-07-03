package org.example;

import com.sun.security.auth.module.LdapLoginModule;

import javax.naming.directory.DirContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.net.Socket;
import java.util.concurrent.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Node extends UnicastRemoteObject implements NodeInterface {
    private String name, Registry;
    private NodeInterface root, parentNode;
    private ArrayList<NodeInterface> children;
    private LLMProcess process;
    private DirContext ldapContext;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private int requestCounter = 0;
    private PrintWriter outStream;
    private BufferedReader inStream;
    private Socket serverSocket;

    public Node(String name, String Registry, String rootName) throws Exception{
        this.name= name;
        this.Registry = Registry;
        children = new ArrayList<>();
        process = new LLMProcess();
        root = getRemoteNode(rootName);
        ldapContext = Utilities.createLdapContext(this.Registry);

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            process.destroyProcess();
        }));
    }
    private NodeInterface getRemoteNode(String nodeName) {
        try {
            MarshalledObject<NodeInterface> marshalledObject = (MarshalledObject<NodeInterface>) ldapContext.lookup("cn=" + nodeName + ",ou=Nodes");
            NodeInterface remoteNode = marshalledObject.get();
            return remoteNode;
        } catch (Exception e){
            return null;
        }
    }
    public void connectParent(String parentName) throws RemoteException{
        parentNode = getRemoteNode(parentName);
        assert parentNode != null;
        parentNode.connectChild(name);
        System.out.println("parent node connected");
    }
    public void connectChild(String childName) throws RemoteException{
        children.add(getRemoteNode(childName));
    }
    public String log() throws  Exception{
        return String.format("Name:%s\nRegistry:%s\nIP:%s\nParent:%s\nChildren:%s", name, Registry, getIP().getHostAddress(),
                parentNode, children);
    }
    public InetAddress getIP() throws Exception {
        return InetAddress.getLocalHost();
    }
    public synchronized void receiveMessages(String incomingMsg) throws RemoteException{
        if(executor.getActiveCount() == 0)
            consultLLM(incomingMsg);
        else
            if(children.isEmpty())
                consultLLM(incomingMsg);
            else
                children.get(requestCounter).receiveMessages(incomingMsg);
                requestCounter = (requestCounter +1 ) % children.size();
    }
    private synchronized void receiveMessagePython(String incomingMsg) throws Exception{ // Only for the Root Node
        children.get(requestCounter).receiveMessages(incomingMsg);
        requestCounter = (requestCounter + 1) % children.size();
    }
    public void consultLLM(String userQuery) {
        executor.execute(() -> {
            try {
                String resolvedQuery = process.sendQuery(userQuery);
                root.sendMessagePython(resolvedQuery);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

        });

    }
    public void sendMessagePython(String outgoingMsg) throws Exception{
        outStream.println(outgoingMsg);
        outStream.flush();
    }
    public void connectServer(String host, int port) throws Exception {
        if(serverSocket != null)
            serverSocket.close();
        Thread serverThread = new Thread(()-> {
            try{
                serverSocket = new Socket(host, port);
                outStream = new PrintWriter(serverSocket.getOutputStream());
                outStream.flush();
                inStream = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

                String message;
                while ((message = inStream.readLine()) != null){
                    receiveMessagePython(message);
                }
                serverSocket.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
        serverThread.start();
        
    }
}
