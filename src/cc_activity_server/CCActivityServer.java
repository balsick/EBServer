package cc_activity_server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import cc_activity_server.http.EBHTTPServer;


public class CCActivityServer {
	
	public static int port = 5432;
	public static Logger logger;
	
	public static void main(String[] args){
		String logPath = "log";
		File d = new File(logPath);
		if (d.exists() == false)
			d.mkdir();
		String logFilePath = "log\\log.log";
		File f = new File(logFilePath);
		Path source = Paths.get(logFilePath);
		if (f.exists()){
			String destPathName = "log\\log"+f.lastModified()+".log";
			Path dest = Paths.get(destPathName);
			try {
				Files.move(source, dest);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		EBHTTPServer serverHTTP;
		try (
				ServerSocket serverSocket = new ServerSocket(port);
				){
			int i = 0;
			logger = Logger.getLogger("MyLog");
			FileHandler fh = new FileHandler(logFilePath);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
			logger.info("Server started");
			System.out.println("Server started");
//			List<Socket> clientSockets = new ArrayList<>();
			serverHTTP = new EBHTTPServer();
			serverHTTP.start();
			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println((i++) + "\tConnection accepted by " + clientSocket.getRemoteSocketAddress());
				logger.info((i++) + "\tConnection accepted by " + clientSocket.getRemoteSocketAddress());
//				clientSockets.add(clientSocket);
				CommunicationThread thread = new CommunicationThread(clientSocket);
				thread.start();
			}
		}catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
