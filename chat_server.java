
/*
 * Implementation of a conference server in java
 * By Srihari Nelakuditi for CSCE 416
 */

// Package for I/O related stuff
import java.io.*;

// Package for socket related stuff
import java.net.*;

// Package for list related stuff
import java.util.*;


/*
 * This class does all the conf server's job
 * 
 * It consists of parent thread (code inside main method) which accepts
 * new client connections and then spawns a thread per connection
 * 
 * Each child thread (code inside run method) reads messages
 * from its socket and broadcasts the message to the all active connections
 * 
 * Since a thread is being created with this class object,
 * this class declaration includes "implements Runnable"
 */
public class chat_server implements Runnable
{
	// Each instance has a separate socket
	private Socket clientSock;
	private static  boolean availableClients = false;
	// Whole class keeps track of active clients
	private static List<PrintWriter> clientList;
	private static List<client> clients_List;
	private static boolean disableThis = false;
	static String name = "";
	static String otherName = "";
	// Constructor sets the socket for the child thread to process
	public chat_server(Socket sock)
	{
		clientSock = sock;
	}
	
	// Add the given client to the active clients list
	// Since all threads share this, we use "synchronized" to make it atomic
	public static synchronized boolean addClient(PrintWriter toClientWriter, client this_client)
	{
		clients_List.add(this_client);
		return(clientList.add(toClientWriter));
	}

	// Remove the given client from the active clients list
	// Since all threads share this, we use "synchronized" to make it atomic
	public static synchronized boolean removeClient(PrintWriter toClientWriter)
	{
		return(clientList.remove(toClientWriter));
	}

	// Relay the given message to all the active clients
	// Since all threads share this, we use "synchronized" to make it atomic
	public static synchronized void relayMessage(String mesg)
	{
		// Iterate through the list and send message to each client
		int index = 0;
		
		for (PrintWriter clientWriter : clientList) {
			//System.out.println(clients_List.get(index).getName() + " : " + clients_List.get(index).getStatus().equalsIgnoreCase("free"));
			if((mesg.equalsIgnoreCase("y") == false) && ((mesg.equalsIgnoreCase("silent")==false) && (clients_List.get(index).getStatus().equalsIgnoreCase("free") == true)))
			{
				//System.out.println("printing to " + clients_List.get(index).getName() + "...");
				clientWriter.println(mesg);
			}
			index++;
		}
	}
	public static synchronized void relayMessageToTheseClients(String mesg, String name1, String name2)
	{
		//System.out.println("Name 1: " + name1 + "\nName 2:" + name2);
		//name 1 name 2 system doesnt work as more people connect,
		//we need to instead define who a client is talking to in their object
		for(int i = 0; i < clients_List.size(); i++)
		{
			if((clients_List.get(i).getName().equalsIgnoreCase(name1) || (clients_List.get(i).getName().equalsIgnoreCase(name2))))
				if(!(mesg.equalsIgnoreCase("y")) && !(mesg.equalsIgnoreCase("silent")))
				{
					clientList.get(i).println(mesg);
				}
		}

	}

	// The child thread starts here
	public void run()
	{
		// Read from the client and relay to other clients
		try {

			// Prepare to read from socket
			BufferedReader fromClientReader = new BufferedReader(
					new InputStreamReader(clientSock.getInputStream()));
			
			// Get the client name
			String clientName = fromClientReader.readLine();
			name = clientName;
			System.out.println(clientName + " joined the conference");

			// Prepare to write to socket with auto flush on
			PrintWriter toClientWriter =
					new PrintWriter(clientSock.getOutputStream(), true);
			
			// Add this client to the active client list
			client this_client = new client(clientName, "free", clientSock);
			addClient(toClientWriter, this_client);
			
			//send out the current status info
			System.out.println("Client accepted ");
			toClientWriter.println("Welcome to the 416 chat server"); //welcome the specific user to the server
			printClients();
			relayMessage(getClients());
			if(availableClients == true)
			{
				relayMessage("Clients are available for connection");
				
			}
			// Keep doing till client sends EOF
			while (true) {
				// Read a line from the client
				//System.out.println("SERVER: AT: " + "fromclientreader readline");
				String line = "";
				if(disableThis == false)
					line = fromClientReader.readLine();
				// If we get null, it means client sent EOF
				if (line == null)
					break;
				//check to see if the user entered the name of another user
				for(int i = 0; i < clients_List.size(); i++) // I is the index of the user being talked to.
				{	
					if(line.equalsIgnoreCase(clients_List.get(i).getName()))
					{
						otherName = clients_List.get(i).getName();
						//System.out.println("Othername: " + otherName);
						toClientWriter.println("Sent Connection Request to " +  clients_List.get(i).getName());
						PrintWriter OutputToOtherUser = new PrintWriter(clientList.get(i), true);
						OutputToOtherUser.println("Received request from " +  clientName + "\nConnect? (y/n)");
						BufferedReader fromOtherClientReader = new BufferedReader(
								new InputStreamReader(clients_List.get(i).getport().getInputStream()));
						//System.out.println("SERVER: AT: " + "fromclientreader readline 2 2 2");
						if(fromOtherClientReader.readLine().contains("y"))
						{
							toClientWriter.println("You are connected to " + clients_List.get(i).getName());
							OutputToOtherUser.println("You are connected to " + clientName);
							clients_List.get(i).setStatus("busy"); //set other user to busy
							for(int b = 0; b < clients_List.size(); b++)
							{
								if(clients_List.get(b).getName().equalsIgnoreCase(clientName))
								{
									clients_List.get(b).setStatus("busy"); //set user to busy
									clients_List.get(i).setConnectedWith(clients_List.get(b).getName());
									clients_List.get(b).setConnectedWith(clients_List.get(i).getName());


								}
							}
							printClients();
							relayMessage(getClients());
						}
					}
				}
				
				
				// Send the line to all active clients
				String fixedLine = clientName + ": " + line;
				if(!(line.equalsIgnoreCase("y")) && !(line.equalsIgnoreCase(otherName) ))
				{
					for(int i = 0; i < clients_List.size(); i++)
					{
						if(clients_List.get(i).getName().equalsIgnoreCase(clientName))
							relayMessageToTheseClients(fixedLine, clientName, clients_List.get(i).getConnectedWith());
					}

					//relayMessage(fixedLine);
				}
			}

			
			// Done with the client, close everything
			toClientWriter.close();
			System.out.println(name + " has disconnected ");
			try {
				//System.out.println("0");
				//System.out.println("Othername: " + otherName);
			for(int i = 0; i < clients_List.size(); i++) 
			{	
				//System.out.println("1");
				if(otherName.equalsIgnoreCase(clients_List.get(i).getName()))
				{
					//System.out.println("2");
					PrintWriter OutputToOtherUser = new PrintWriter(clientList.get(i), true);
					OutputToOtherUser.println(name + " has disconnected"); //sends to non-disconnected user
					clients_List.get(i).setStatus("free");
					for(int c = 0; c < clients_List.size(); c++) 
					{	
						//System.out.println("3");
						if(name.equalsIgnoreCase(clients_List.get(c).getName()))
						{
							clients_List.get(c).setStatus("free");
							clients_List.remove(c);
							clientList.remove(c);
						}
					}
					//printClients();
					OutputToOtherUser.println(getClients());
				}
			}
			// Remove this client from active list
			removeClient(toClientWriter);
			printClients();
			relayMessage(getClients());
			}catch(Exception d) {}
		}
		catch (Exception e) {
			
		}
	}

	/*
	 * The conf server program starts from here.
	 * This main thread accepts new clients and spawns a thread for each client
	 * Each child thread does the stuff under the run() method 
	 */
	public static void main(String args[])
	{
		// Server needs a port to listen on
		if (args.length != 1) {
			System.out.println("usage: java ConfServer <port>");
			System.exit(1);
		}

		// Be prepared to catch socket related exceptions
		try {
			// Create a server socket with the given port
			ServerSocket serverSock = 
					new ServerSocket(Integer.parseInt(args[0]));
			System.out.println("Waiting for clients ...");
			
			// Keep track of active clients
			clientList = new ArrayList<PrintWriter>();
			clients_List = new ArrayList<client>();
			
			// Keep accepting/serving new clients
			while (true) {
				// Wait for another client
				Socket clientSock = serverSock.accept();
				
				// Spawn a thread to read/relay messages from this client
				Thread child = new Thread(new chat_server(clientSock));
				child.start();
			}
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}
	public static void printClients()
	{
		System.out.println("List of clients and states:");
		for(int i = 0; i < clients_List.size(); i++)
		{
			System.out.println(clients_List.get(i).getName() + "              " + clients_List.get(i).getStatus());
		}
	}
	public static String getClients()
	{
		String returnString = "List of clients and states:\n";
		for(int i = 0; i < clients_List.size(); i++)
		{
			returnString = returnString + ((clients_List.get(i).getName() + "              " + clients_List.get(i).getStatus()).toString() + "\n");
		}
		//check for any available connections
		boolean availableConnectionsMe = false;
		boolean availableConnectionsNotMe = false;
		for(int i = 0; i < clients_List.size(); i++)
		{
			if(clients_List.get(i).getStatus().equalsIgnoreCase("free") && availableConnectionsMe == false)
			{
				availableConnectionsMe = true;
				continue;
			}
			if(clients_List.get(i).getStatus().equalsIgnoreCase("free") && availableConnectionsMe == true)
			{
				availableConnectionsNotMe = true;
				continue;
			}
		}
		if(availableConnectionsNotMe == false) {
			returnString = returnString + "No available peers currently for connection";
			availableClients = false;
		}
		if(availableConnectionsNotMe == true) //there is a client this client can connect to
		{
			//returnString = returnString + "Clients are available for connection\nConnect to which client? (do nothing to continue searching)";
			availableClients = true;
		}
		
		return returnString;
	}
	public class client {
		private String name = "ERROR: no name set";
		private String status = "ERROR: no status set";
		private String connectedWith = "ERROR: no status set";
		private Socket sock;
		
		public client(String name, String status, Socket sock) {
			this.name = name;
			this.status = status;
			this.sock = sock;
		}
		public String getName()
		{
			return this.name;
		}

		public String getStatus()
		{
			return this.status;
		}
		public String getConnectedWith()
		{
			return this.connectedWith;
		}
		public Socket getport()
		{
			return this.sock;
		}
		public void setStatus(String status)
		{
			if(status.equalsIgnoreCase("busy") || status.equalsIgnoreCase("free"))
				this.status = status;
			else
				System.out.println("Error: Status must be either busy or free");
		}
		public void setConnectedWith(String clientName)
		{
			this.connectedWith = clientName;
		}
	}
}