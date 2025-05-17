package org.redis;

import java.io.*;
import java.net.Socket;

public class ClientDriver {

    public static void main(String[] args) throws IOException {
        Socket clientSocket = new Socket("127.0.0.1", 7878);
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        out.write(1);
        out.write(3);
        out.writeInt(3);
        out.write('s');
        out.write('e');
        out.write('t');
        out.writeInt(3);
        out.write('o');
        out.write('o');
        out.write('m');
        out.writeInt(4);
        out.write('y');
        out.write('e');
        out.write('s');
        out.write('s');
        out.flush();
        out.close();



    }
}
