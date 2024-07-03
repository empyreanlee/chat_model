package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class LLMProcess {
    private  final Process process;
    private final int port;

    public LLMProcess() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();

        process = new ProcessBuilder("python", "Module/llm.py", String.valueOf(port)).start();

    }
    public String sendQuery(String query) throws Exception{
        Socket pythonSocket = new Socket("localhost", port);
        PrintWriter pyoutput = new PrintWriter(pythonSocket.getOutputStream(), true);
        BufferedReader pyinput = new BufferedReader(new InputStreamReader(pythonSocket.getInputStream()));

        pyoutput.println(query);
        pyoutput.flush();
        String response = pyinput.readLine();
        pythonSocket.close();
        return response;
    }
    public void destroyProcess(){
        process.destroyForcibly();
    }
}
