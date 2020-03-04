package net.raysforge.tunnel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Tunnel {

	private InputStream sis;
	private OutputStream sos;
	private InputStream cis;
	private OutputStream cos;
	
	public final static int MY_PORT = 1234; //  4444; // 27019;
	public final static String THEIR_HOSTNAME = "localhost"; // "172.30.15.222"; // "localhost";
	public final static int THEIR_PORT = 27017; // 4444; // 27017; // 6597
	public final static boolean LOG_INPUT = true;

	@SuppressWarnings("resource")
	public Tunnel(Socket client) throws IOException {
		
		Socket server = new Socket(THEIR_HOSTNAME, THEIR_PORT);
		
		sis = server.getInputStream();
		sos = server.getOutputStream();
		
		cis = client.getInputStream();
		cos = client.getOutputStream();
		
		//new StreamPump(sis, cos, LOG_INPUT, false).start();
		//new StreamPump(cis, sos, !LOG_INPUT, false).start();
		new StreamPump(sis, cos, new FileOutputStream("fromServer2Client_NEW5.bin")).start();
		new StreamPump(cis, sos, new FileOutputStream("fromClient2Server_NEW5.bin")).start();
	}
	

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ServerSocket ss = new ServerSocket(MY_PORT);
		
		while(true) {
			new Tunnel( ss.accept());
		}
		
		
	}

}
