package org.example;
import java.net.*;
import java.rmi.*;

public interface NodeInterface extends Remote {
    InetAddress getIP() throws Exception;
    String log() throws Exception;
    void receiveMessages(String incomingMsg) throws RemoteException;
    void sendMessagePython(String outgoingMsg) throws Exception;
    void connectParent(String parentName) throws RemoteException;
    void connectChild(String childName) throws RemoteException;
}

