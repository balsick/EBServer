package cc_activity_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.balsick.tools.communication.ClientServerDBResult;
import com.balsick.tools.utils.GeneralUtils;

public class CommunicationThread extends Thread {
	final Socket socket;
	private Logger logger;
	
	public CommunicationThread(Socket c) {
		super("CommunicationThread");
		this.socket = c;
	}
	
	public String getIP() {
		if (socket == null)
			return null;
		return socket.getInetAddress().getHostAddress();
	}
	
	@Override
	public void run() {
		if (socket == null)
			return;
		try (
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				) {
			out.println("Connection accepted");
			out.println("OK");
			logger.info("Started thread");
			System.out.println("Started thread");
			String input;
			while (!socket.isClosed() && ((input = in.readLine())) != null) {
//				input = in.readLine();
//				if (input == null)
//					{
//					Thread.sleep(1000);
//					out.println("Waiting 1000ms more");
//					continue;
//					}
				System.out.println("Input from client: "+input);
				logger.info("Input from client: "+input);
//				out.println(input);
				if (isCloseConnectionRequest(input)) {
					out.println("Connection closed");
					System.out.println("Connection closed. Cya");
					logger.info("Connection closed. Cya");
					break;
				}
				if (isDBRequest(input)) {
					manageDataRequest(in);
				}
			}
			if (!socket.isClosed())
				socket.close();
			System.out.println("Closing socket");
			logger.info("Closing socket");
		} catch (IOException ioex) {
			ioex.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
		}
	}
	
	public void setLog(Logger logger) {
		this.logger = logger;
	}
	
	public boolean isCloseConnectionRequest(String request) {
		return "byebye".equalsIgnoreCase(request);
	}
	
	public boolean isDBRequest(String request) {
		return "request_data_start".equalsIgnoreCase(request);
	}

	private void manageDataRequest(BufferedReader in) {
		String line;
		HashMap<String, List<String>> args = new HashMap<>();
		String[] columns = null;
		String table = null;
		String[] criteria = null;
		String[] groupby = null;
		String[] orderby = null;
		try {
			while ((line = in.readLine()) != null && !line.equals("request_data_end")) {
				if (line.matches("^columns=\\{.*\\}"))
					args.put("columns", GeneralUtils.translate(line, "columns"));
				else if (line.matches("^table=\\{?.*\\}?"))
					args.put("table", GeneralUtils.translate(line, "table"));
				else if (line.matches("^criteria=\\{.*\\}"))
					args.put("criteria", GeneralUtils.translate(line, "criteria"));
				else if (line.matches("^groupby=\\{.*\\}"))
					args.put("groupby", GeneralUtils.translate(line, "groupby"));
				else if (line.matches("^orderby=\\{.*\\}"))
					args.put("orderby", GeneralUtils.translate(line, "orderby"));
			}
		} catch (IOException ex){
			
		}
		try {
			ClientServerDBResult result = DBManager.select(args, this);
//			ClientServerDBResult result = DBManager.select(columns, table, criteria, groupby, orderby, this);
			ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
			outputStream.flush();
			outputStream.writeObject(result);
//			outputStream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
