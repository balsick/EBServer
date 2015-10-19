package cc_activity_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

import com.balsick.tools.communication.ClientServerResult;
import com.balsick.tools.communication.JSonParser;
import com.balsick.tools.utils.GeneralUtils;

public class CommunicationThread extends Thread {
	final Socket socket;
	
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
			log("Started thread");
			String input;
			while (!socket.isClosed() && ((input = in.readLine())) != null) {
//				input = in.readLine();
//				if (input == null)
//					{
//					Thread.sleep(1000);
//					out.println("Waiting 1000ms more");
//					continue;
//					}
				log("Input from client: "+input);
				if (isCloseConnectionRequest(input)) {
					out.println("Connection closed");
					log("Connection closed. Cya");
					break;
				}
				if (isDBRequest(input)) {
					manageDataRequest(in);
				}
			}
			if (!socket.isClosed())
				socket.close();
			log("Closing socket");
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
	
	private void log(String s) {
		System.out.println(s);
		if (CCActivityServer.logger != null)
			CCActivityServer.logger.info(s);
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
		boolean json = false;
		try {
			while ((line = in.readLine()) != null && !line.equals("request_data_end")) {
				if (line.matches("^columns=\\{.*\\}"))
					args.put("columns", GeneralUtils.translate(line, "columns"));
				else if (line.matches("^tables=\\{?.*\\}?"))
					args.put("tables", GeneralUtils.translate(line, "tables"));
				else if (line.matches("^criteria=\\{.*\\}"))
					args.put("criteria", GeneralUtils.translate(line, "criteria"));
				else if (line.matches("^groupby=\\{.*\\}"))
					args.put("groupby", GeneralUtils.translate(line, "groupby"));
				else if (line.matches("^orderby=\\{.*\\}"))
					args.put("orderby", GeneralUtils.translate(line, "orderby"));
				else if (line.equals("json_request=yes"))
					json = true;
			}
		} catch (IOException ex){
			log("impossible reading from stream");
		}
		try {
			ClientServerResult result = DBManager.select(args, this);
			ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
			outputStream.flush();
			outputStream.writeObject(json ? JSonParser.getJSon(result) : result);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
