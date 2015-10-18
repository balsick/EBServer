package cc_activity_server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.balsick.tools.communication.ClientServerDBResult;
import com.balsick.tools.communication.ClientServerDBResultRow;
import com.balsick.tools.communication.ClientServerMessageResult;
import com.balsick.tools.communication.ClientServerResult;
import com.balsick.tools.communication.ColumnStructure;
import com.balsick.tools.communication.JSonParser;

public class DBManager {
	
	public static DBManager current = null;
	Connection connection;
	
	private DBManager() {
		
	}
	
	private Connection getConnection() {
		if (connection == null)
			try {
				Class.forName("com.mysql.jdbc.Driver");
				connection = DriverManager.getConnection("jdbc:mysql://localhost/"+Info.DBName+"?"+"user="+Info.DBid+"&password="+Info.DBpassword);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		return connection;
	}
	
	public static boolean checkTableExistence(String table) {
		try {
			if (selectResultSet("select * from " + table) == null)
				return false;
		} catch (Exception ex) {
			return false;
		}
		return true;
	}
	
	public static ResultSet selectResultSet(String query) {
		ResultSet rs = null;
		try (PreparedStatement preparedStatement = getCurrent().getConnection().prepareStatement(query);){
			rs = preparedStatement.executeQuery();
		} catch (SQLException ex) {
			
		}
		return rs;
	}
	
	public static ClientServerResult select(List<String> columns, List<String> table, List<String> criteria, List<String> groupby, List<String> orderby) throws Exception {
		return select(columns, table, criteria, groupby, orderby, null);
	}
	
	public static ClientServerResult select(List<String> columns, List<String> table, List<String> criteria, List<String> groupby, List<String> orderby, Object source) throws Exception {
		if (table == null)
			throw new Exception("missing table name");
		String query = "select ";
		String separator = ",";
		if (columns != null) {
			for (String c : columns) {
				query += c + separator;
			}
			query = query.substring(0, query.length()-separator.length());
		} else query += "*";
//		query += " from "+table;
		query += " from ";
		for (String c : table) {
			query += c + separator;
		}
		query = query.substring(0, query.length()-separator.length());
		separator = " and ";
		if (criteria != null) {
			query += " where ";
			for (String c : criteria) {
				query += c + separator;
			}
			query = query.substring(0, query.length()-separator.length());
		}
		separator = ",";
		if (groupby != null) {
			query += " group by ";
			for (String c : groupby) {
				query += c + separator;
			}
			query = query.substring(0, query.length()-separator.length());
		}
		if (orderby != null) {
			query += " order by ";
			for (String c : orderby) {
				query += c + separator;
			}
			query = query.substring(0, query.length()-separator.length());
		}
		return select(query, source);
	}
	
	public static ClientServerResult select(String query) {
		return select(query, null);
	}
	
	public static ClientServerResult select(Map<String, List<String>> args, Object source) throws Exception {
		return select(args.get("columns"), args.get("tables"), args.get("criteria"), args.get("groupby"), args.get("orderby"), source);
	}
	
	public static ClientServerResult select(String query, Object source) {
//		List<HashMap<String, Object>> results = new ArrayList<>();
		String info = "Executing query:\n"+query;
		if (source instanceof CommunicationThread) {
			info += "\nSource:\t"+((CommunicationThread)source).getIP();
		}
		System.out.println(info);
		CCActivityServer.logger.info(info);
		ClientServerResult result = null;
		try (
				PreparedStatement preparedStatement = getCurrent().getConnection().prepareStatement(query);
				ResultSet rs = preparedStatement.executeQuery();
				){
			result = new ClientServerDBResult();
			ResultSetMetaData meta = rs.getMetaData();
			int l = meta.getColumnCount();
			for (int i = 1; i <= l; i++) {
				String columnlabel = meta.getColumnName(i);
				String type = meta.getColumnTypeName(i);
				ColumnStructure cs = new ColumnStructure(columnlabel, type+"");
				((ClientServerDBResult)result).addColumn(cs);
			}
			while (rs.next()) {
				ClientServerDBResultRow row = new ClientServerDBResultRow();
				for (int i = 1; i <= meta.getColumnCount(); i++){
					Object obj = rs.getObject(i);
					row.put(meta.getColumnName(i), obj);
				}
				((ClientServerDBResult)result).addRow(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			result = new ClientServerMessageResult(ClientServerResult.RESULTFAIL, "sql error:\n"+e.getMessage());
//			return result;
		} catch (NullPointerException e) {
			e.printStackTrace();
			result = new ClientServerMessageResult(ClientServerResult.RESULTFAIL, "error:\n"+e.getMessage());
//			return result;
		}
		String json = JSonParser.getJSon(result);
		System.out.println(json);
		return result;
	}
	
	public static DBManager getCurrent() {
		if (current == null)
			current = new DBManager();
		return current;
	}

}
