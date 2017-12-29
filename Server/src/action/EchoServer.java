package action;

// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 
 
import java.io.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import java.sql.PreparedStatement;
import ocsf.server.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */
public class EchoServer extends AbstractServer 
{
  //Class variables *************************************************
  
  /**
   * The default port to listen on.
   */
  final public static int DEFAULT_PORT = 5555;
   public static String user_pass;
  public static String table_name;
  public static String schema_name;
  public static String user_name;
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port) 
  {
    super(port); 
    user_name=JOptionPane.showInputDialog("Enter User name  ");
     if(user_name.equals(""))
	   {
	 	   JOptionPane.showMessageDialog(null, "Invalid name");
	       System.exit(0);
	   }
     user_pass=JOptionPane.showInputDialog("Enter Password  ");
     schema_name = JOptionPane.showInputDialog("Enter Schema Name ");
  
    
  } 

  
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   * This method check which kind of query arrived from client
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient(Object msg, ConnectionToClient client)
  {	 
	  
	  		 Msg msg1=(Msg)msg;
	  		 String query_type=msg1.getType();
	       try 
			{
	          Class.forName("com.mysql.jdbc.Driver").newInstance();
	        } catch (Exception ex) {/* handle the error*/}
	        
	        try 
	        {	 
	            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/"+schema_name,user_name,user_pass);      
	            System.out.println("SQL connection succeed");
	           
	            
	            
	            
	             /* Define which kind the message the server got */
	            /**
	             * first find the kind of the query 
	             * then, check the role of the msg (the role is a simple short string)
	             */
	           switch(query_type)
	           {
	           case  "SELECT":{
	           
	        	   if(msg1.getRole().equals("verify user details"))
	        		  check_user_details(msg1,conn,client);  
	           }
	           case  "UPDATE":{
	            
	        	   if(msg1.getRole().equals("user logout"))
	        	   change_online_status(msg1 ,conn,"0");
	        	   if(msg1.getRole().equals("update user details"))
	        		   Update_user_details(msg1,conn,client);
	        	  // else  getProdectdetails(msg1,conn,client);
	        	  // else  UpdateItem(conn,msg,client);
	           }
	           
	           case  "SELECTALL":
	           { 
	        	     ViewItems(conn,client);
	           }
	           
	           
	           }// end switch
	            
	           
	        }//end try   
		        	  
	        catch (SQLException ex) {
	     	     /* handle any errors*/
	             System.out.println("SQLException: " + ex.getMessage());
	             System.out.println("SQLState: " + ex.getSQLState());
	             System.out.println("VendorError: " + ex.getErrorCode());
	        }      
	          
}  
  public static void check_user_details(Msg msg1 ,Connection conn,ConnectionToClient client)
	{
		 Person user=(Person) msg1.oldO;
		 String a;
		   try {
			   PreparedStatement ps=conn.prepareStatement(" SELECT * FROM "+msg1.getTableName()+" "+"WHERE FirstName=? and Password=?;"); 
				
				/*insert the names to the query*/
				ps.setString(1,user.getUser_name());
				ps.setString(2,user.getUser_password());
				ResultSet rs=ps.executeQuery();	
		
				while(rs.next()) {
					a=rs.getString(1);
					
				// if the user exist
			   if(!(a.equals("0")))
			   {
				   user.setIsExist("1");
				   user.setUser_ID(rs.getString(1));
				   user.setPrivilege(rs.getString(5));
				   user.setUser_last_name(rs.getString(3));
				   user.setWWID(rs.getString(7));
				   user.setIsOnline("1");
				   msg1.oldO=user;
				   
				   /*check if it is possible to change the status of the user*/
					if(change_online_status(msg1 ,conn,"1")== true)
						{
							msg1.newO=(Person)user;
						   client.sendToClient(msg1);
						   return;
						}
					else {// if the user is already connected
						
						msg1.newO=(Person)user;
						user.setAlreadyConnected(true);
						client.sendToClient(msg1);
						return;
						}
			   	}// end if
		
					}//while rs	

				if(rs.next()==false) 	   /*if the user dosent exist in the system*/  
				   {
					 Person user_not_exist= new Person(null,null,null);
					 user_not_exist.setIsExist("0");
					 msg1.newO=user_not_exist;
					 client.sendToClient(msg1);
					 return;
				
				   }
		}//try

		      catch (SQLException e) {e.printStackTrace();}
		   catch(IOException x) {System.err.println("unable to send msg to client");}
	}
  
  public static boolean change_online_status(Msg msg1 ,Connection conn,String new_status)
  {
		 Person user=(Person) msg1.oldO;
		  boolean answer;
		  answer= isConnected(msg1,conn);
		  if(answer==true && new_status.equals("1"))
			  return false;
		  
		   try {		
				   PreparedStatement ps=conn.prepareStatement("UPDATE "+msg1.getTableName()+" "+"SET Online=? WHERE ID="+user.getUser_ID()); 
				   ps.setString(1,new_status);
				   ps.executeUpdate(); 
				    
				 //  System.out.println("the online status was changed ");
			 }	  
		 
		   
		      catch (SQLException e) {e.printStackTrace();}
		return true;
  }
  
   
  public static boolean isConnected( Msg msg1 ,Connection conn)
  {	 
	  boolean isAlreadyCon=false;
		 Person user=(Person) msg1.oldO;
		 String current_status;
		 
		   try {
			   PreparedStatement ps=conn.prepareStatement("SELECT Online FROM "+schema_name+"."+msg1.getTableName()+" "+" WHERE ID="+user.getUser_ID()); 
				ResultSet rs=ps.executeQuery();	
				while(rs.next())
				{
					current_status=rs.getString(1);
					if(current_status.equals("1"))	
					{
						isAlreadyCon=true;
						//System.out.println("cannot change the user status- user is already online");
						return isAlreadyCon;
					}
				}
		   }
		      catch (SQLException e) {e.printStackTrace();} 
						return isAlreadyCon;
	   
  }

	public static void Update_user_details(Object msg,Connection con,ConnectionToClient client)
	{
		String ans="Update done";
		 Msg msg1 =(Msg)msg;
		 Person  old_d=(Person) msg1.oldO;
		 Person  new_d=(Person)msg1.newO;
		 try {
		PreparedStatement ps=con.prepareStatement("UPDATE "+msg1.getTableName()+" "+"SET FirstName=? , LastName=? , Password=? WHERE ID=?");
		
		/*insert the names to the query*/
		ps.setString(1,new_d.getUser_name());
		ps.setString(2,new_d.getUser_last_name());
		ps.setString(3,new_d.getUser_password());
	 	ps.setString(4, old_d.getUser_ID());
		 
		ps.executeUpdate(); 
		
		client.sendToClient(ans);
		
		 }
		 catch (SQLException e) {
			 e.printStackTrace();
		 try {
			client.sendToClient("UPDATE faild");
		} catch (IOException e1) {
			e1.printStackTrace();
		}}
		catch(IOException x) {System.err.println("unable to send msg to client");}

	}
  
  
  
  
  
  
	   /**
	    * send a query to DB ->Select all
	    * returns the names of the products as an Object(ArrayList<String>)
	    * @param con
	    * @param client
	    */
	public static void ViewItems(Connection con ,ConnectionToClient client)
	{
		 Statement stmt;
		   Object temp= new ArrayList<String>() ;   
		   try {
		    
		   stmt = con.createStatement();
		   ResultSet rs = stmt.executeQuery("SELECT Name FROM zerli.item");
		    while(rs.next())
		    {
		     ((ArrayList<String>) temp).add(rs.getString(1));    
		    }
		    client.sendToClient(temp);
		   }
		      catch (SQLException e) {e.printStackTrace();}
		    catch(IOException x) {System.err.println("unable to send msg to client");}
	}
  
	/**
	 * this method gets the product name and change it.
	 * gets two Objects(product)
	 * @param con
	 * @param msg
	 * @param client
	 */
	public static void UpdateItem(Connection con,Object msg,ConnectionToClient client)
	{
		String ans="Update done";
		Msg msg1=(Msg)msg;
		 Product p_old=(Product) msg1.oldO;
		 Product p_new=(Product)msg1.newO;
		 try {
		PreparedStatement ps=con.prepareStatement("UPDATE hw2.product SET ProductId=?,ProductName=?,ProductType=? WHERE ProductName=?"); 
		
		/*insert the names to the query*/
		ps.setString(1,p_new.GetID());
		ps.setString(2,p_new.GetName());
		ps.setString(3,p_new.GetType());
	 	ps.setString(4,p_old.GetName());
		 
		ps.executeUpdate(); 
		
		client.sendToClient(ans);
		
		 }
		 catch (SQLException e) {
			 e.printStackTrace();
		 try {
			client.sendToClient("UPDATE faild");
		} catch (IOException e1) {
			e1.printStackTrace();
		}}
		catch(IOException x) {System.err.println("unable to send msg to client");}

	}
	/**
	 * this method gets the name of a product 
	 * sends back the products details
	 * @param msg
	 * @param con
	 * @param client
	 */
	public static void getProdectdetails(Object msg,Connection con,ConnectionToClient client)
	{		  
		Msg msg1=(Msg)msg;		
		Product p= (Product)msg1.oldO;		
		try { 
		
			/*send a query with the product name as a parameter*/
			PreparedStatement ps = con.prepareStatement("SELECT * FROM hw2.product where ProductName=?");			
			ps.setString(1,p.GetName());			
			ResultSet r=ps.executeQuery();				
			 while(r.next())
			 { 		
				 p.SetID(r.getString(1));
				 p.SetName(r.getString(2));
				 p.SetType(r.getString(3));				
				 msg1.oldO=p;				
			 } 
			 /*back to client*/
			client.sendToClient(msg1);
		 	r.close();
			}
		  catch(IOException x) {System.err.println("unable to send msg to client");}
		  catch (SQLException ex) 
			
		    {/* handle any errors*/
	    System.out.println("SQLException: " + ex.getMessage());
	    System.out.println("SQLState: " + ex.getSQLState());
	    System.out.println("VendorError: " + ex.getErrorCode());
	    }
		}
		
/**
   * This method overrides the one in the superclass.  Called
   * when the server starts listening for connections.
   */
  protected void serverStarted()
  {
    System.out.println
      ("Server listening for connections on port " + getPort());
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when the server stops listening for connections.
   */
  protected void serverStopped()
  {
    System.out.println
      ("Server has stopped listening for connections.");
  }
  
  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of 
   * the server instance (there is no UI in this phase).
   *
   * @param args[0] The port number to listen on.  Defaults to 5555 
   *          if no argument is entered.
   */
  public static void main(String[] args) 
  {
    int port = 0; //Port to listen on
    try
    {
      port = Integer.parseInt(args[0]); //Get port from command line
    }
    catch(Throwable t)
    {
      port = DEFAULT_PORT; //Set port to 5555
    }
	
    EchoServer sv = new EchoServer(port);
    
    try  
    {
      sv.listen(); //Start listening for connections
    } 
    catch (Exception ex) 
    {
      System.out.println("ERROR - Could not listen for clients!");
    }
  }


 
}
//End of EchoServer class
