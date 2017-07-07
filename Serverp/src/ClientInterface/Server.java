package ClientInterface;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JOptionPane;

import DataManagement.RequestManager;

/**
 * @author Yasiru Dahanayake
 * @author Nathan Steer
 */
public class Server
{
	private static String				log						= "client_log.dat";
	private static SimpleDateFormat		time;
	
	private static ServerSocket			sS;
	private static final int			PORT					= 1234;
	private static boolean				serverRunning			= true;
	private static boolean				acceptingConnections	= true;
	private static ObjectOutputStream	oos;
	
	/**
	 * Launch the application.
	 */
	public static void main ( String[] args )
	{
		EventQueue.invokeLater( new Runnable()
		{
			@SuppressWarnings ( "unused" )
			public void run ()
			{
				try
				{
					ServerGUI gui = new ServerGUI();
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}
		} );
		
		SetUpConnections();
	}
	
	/**
	 * Create the application.
	 */
	public Server()
	{
		
	}
	
	public static void toggleConnections ()
	{
		acceptingConnections = !acceptingConnections;
		
		if ( !acceptingConnections )
		{
			try
			{
				System.setProperty( "javax.net.ssl.trustStore", "keystore.jks" );
				SocketFactory factory = SSLSocketFactory.getDefault();
				Socket socket = factory.createSocket( "127.0.0.1", 1234 );
				PrintWriter writer = new PrintWriter( socket.getOutputStream(), true );
				ObjectInputStream inStream = new ObjectInputStream( socket.getInputStream() );
				writer.println( "pausing~0" );
				inStream.readObject();
				socket.close();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				System.out.println( e.getMessage() );
			}
		}
		
		log( "Currently Accepting Connections: " + String.valueOf( acceptingConnections ) );
	}
	
	/**
	 * Uses self signed certificate "ca.store" to authenticate a handshake
	 * Server starts up and listens for connections, if an instance of the
	 * server is already running notify and close the current instance.
	 */
	private static void SetUpConnections ()
	{
		try
		{
			// using a self singed certificate
			// password is capita123
			// String trustStore =
			// Server.class.getResource("Resources").getPath();
			
			System.setProperty( "javax.net.ssl.keyStore", "keystore.jks" );
			System.setProperty( "javax.net.ssl.keyStorePassword", "capita123" );
			ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
			sS = factory.createServerSocket( PORT );
			
			time = new SimpleDateFormat( "dd/MM/yy HH:mm:ss" );
			
			log( "Server running and listening for connections..." );
			while ( serverRunning )
			{
				if ( acceptingConnections )
				{
					System.out.println( "Waiting for connection\n" );
					Socket socket = sS.accept();
					ServerThread rc = new ServerThread( socket );
					Thread tr = new Thread( rc );
					tr.start();
					log( "Client at " + socket.getInetAddress() + " Connected" );
				}
			}
		}
		catch ( BindException e )
		{
			JOptionPane.showMessageDialog( ServerGUI.getsInterface(), "Instance of a server is already running" );
			System.exit( 0 );
		}
		catch ( Exception e )
		{
			log( e.getMessage() );
			e.printStackTrace();
		}
	}
	
	private static String timeStamp ()
	{
		return ( time.format( new Date() ) + " " );
	}
	
	/**
	 * Writes supplied message to log
	 */
	private static void log ( String msg )
	{
		try
		{
			String output = timeStamp() + msg + System.lineSeparator();
			BufferedWriter writer = new BufferedWriter( new FileWriter( log, true ) );
			writer.newLine();
			writer.write( output );
			ServerGUI.getTextArea().append( output );
			writer.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Runnable class to handle instances of clients that are connected test
	 * method handle requests to for testing with dummy client.
	 * 
	 * @author Yasiru Dahanayake
	 * @author Nathan Steer
	 */
	private static class ServerThread implements Runnable
	{
		Socket	socket;
		String	frmClient;
		
		ServerThread( Socket socket )
		{
			this.socket = socket;
		}
		
		@Override
		public void run ()
		{
			try
			{
				ServerGUI.addClient();
				frmClient = readStringFromClient();
				log( frmClient );
				writeToClient( RequestManager.requestMade( frmClient ) );
				socket.close();
				log( "Client " + socket.getInetAddress() + " Disconnected, thread removed" );
				ServerGUI.removeClient();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		
		/**
		 * Writes a List of objects to the client through socket
		 */
		private void writeToClient ( List<Object> list ) throws IOException
		{
			oos = new ObjectOutputStream( socket.getOutputStream() );
			log( list + " sent to client " + socket.getInetAddress() );
			oos.writeObject( list );
		}
		
		/**
		 * reads in a string from the client
		 */
		private String readStringFromClient () throws IOException
		{
			BufferedReader fromClient = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			
			return fromClient.readLine();
		}
	}
}
