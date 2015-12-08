package cc_activity_server.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.balsick.gazzettaparser.FootballParser;
import com.balsick.gazzettaparser.FootballPlayer;
import com.balsick.tools.communication.ClientServerGenericResult;
import com.balsick.tools.communication.ClientServerResult;
import com.balsick.tools.communication.JSonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import cc_activity_server.CCActivityServer;
import cc_activity_server.DBManager;

public class EBHTTPServer extends Thread {
//	private static final String HOSTNAME = "localhost";//212.47.246.70
	private static final int PORT = 8080;
	private static final int BACKLOG = 1;

	private static final String HEADER_ALLOW = "Allow";
	private static final String HEADER_CONTENT_TYPE = "Content-Type";

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final int STATUS_OK = 200;
	private static final int STATUS_METHOD_NOT_ALLOWED = 405;

	private static final int NO_RESPONSE_LENGTH = -1;

	private static final String METHOD_GET = "GET";
	private static final String METHOD_OPTIONS = "OPTIONS";
	private static final String ALLOWED_METHODS = METHOD_GET + "," + METHOD_OPTIONS;

	public void start() {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress((InetAddress)null, PORT), BACKLOG);
		} catch (IOException e) {
			e.printStackTrace();
			CCActivityServer.logger.info("HTTP Server can't start");
			return;
		}
		server.createContext("/selectTest", this::handleEvent);
		server.createContext("/insertTest", this::handleEvent);
		server.createContext("/fantaplayers", this::handleEvent);
		server.start();
		System.out.println("HTTP Server started");
		CCActivityServer.logger.info("HTTP Server started");
	}
	
	public void handleEvent(HttpExchange he){
		try {
			CCActivityServer.logger.info("received something");
			final Headers headers = he.getResponseHeaders();
			for (List<String> list : headers.values())
				for (String s : list)
					CCActivityServer.logger.info(s);
			final String requestMethod = he.getRequestMethod().toUpperCase();
			CCActivityServer.logger.info(requestMethod);
			switch (requestMethod) {
			case METHOD_GET:
				final Map<String, List<String>> requestParameters = getRequestParameters(he.getRequestURI());
				for (String key : requestParameters.keySet()) {
					CCActivityServer.logger.info("Chiave: "+key+"\nvalori:");
					for (String s : requestParameters.get(key))
						CCActivityServer.logger.info(s);
				}
				ClientServerResult result;
				CCActivityServer.logger.info(he.getHttpContext().getPath());
				
				switch (he.getHttpContext().getPath()) {
				case "/selectTest":
					try {
						result = DBManager.select(requestParameters, this);
					} catch (Exception e) {
						result = null;
						e.printStackTrace();
					}
					break;
				case "/fantaplayers":
					FootballParser fp = new FootballParser();
					System.out.println("parsing\t" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
					fp.parse();
					System.out.println("parsed\t" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
//					List<FootballPlayer> players = fp.getPlayers(requestParameters);
					Object players = fp.getPlayers(requestParameters);
					if (players != null)
						{
//						players.keySet().forEach(System.out::println);
						try {
							result = ClientServerGenericResult.createResult(players);
						} catch (IllegalArgumentException ex) {
							result = ClientServerGenericResult.createResult("");
							result.resultType = ClientServerResult.RESULTFAIL;
							ex.printStackTrace();
						}
						break;
						}
					else {
						System.err.println("Nessun giocatore");
					}
				default:
					result = null;
				}
				final String responseBody = JSonParser.getJSon(result, false);
				CCActivityServer.logger.info(responseBody);
				headers.set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));
				final byte[] rawResponseBody = responseBody.getBytes(CHARSET);
				he.sendResponseHeaders(STATUS_OK, rawResponseBody.length);
				he.getResponseBody().write(rawResponseBody);
				System.out.println("finito\t" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
				break;
			case METHOD_OPTIONS:
				headers.set(HEADER_ALLOW, ALLOWED_METHODS);
				he.sendResponseHeaders(STATUS_OK, NO_RESPONSE_LENGTH);
				break;
			default:
				headers.set(HEADER_ALLOW, ALLOWED_METHODS);
				he.sendResponseHeaders(STATUS_METHOD_NOT_ALLOWED, NO_RESPONSE_LENGTH);
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			he.close();
		}
	}

	private static Map<String, List<String>> getRequestParameters(final URI requestUri) {
		final Map<String, List<String>> requestParameters = new LinkedHashMap<>();
		final String requestQuery = requestUri.getRawQuery();
		if (requestQuery != null) {
			final String[] rawRequestParameters = requestQuery.split("[&;]", -1);
			for (final String rawRequestParameter : rawRequestParameters) {
				final String[] requestParameter = rawRequestParameter.split("=", 2);
				final String requestParameterName = decodeUrlComponent(requestParameter[0]);
				List<String> parameters = new ArrayList<>();
				for (final String value : requestParameter[1].split("[,]", -1)) {
					System.out.println("value = "+value);
					final String requestParameterValue = decodeUrlComponent(value);
	//				final String[] requestParameterValues = requestParameterValue.split("|");
	//				parameters.addAll(Arrays.asList(requestParameterValues));
					parameters.add(requestParameterValue);
				}
				requestParameters.put(requestParameterName, parameters);
			}
		}
		return requestParameters;
	}

	private static String decodeUrlComponent(final String urlComponent) {
		try {
			return URLDecoder.decode(urlComponent, CHARSET.name());
		} catch (final UnsupportedEncodingException ex) {
			System.err.println("Wanting to decode this:\t"+urlComponent);
			throw new InternalError();
		}
	}
}