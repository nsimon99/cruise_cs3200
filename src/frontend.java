import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

class Frontend {
  private String destination;
  private String startDate;
  private String endDate;
  private String ans;
  private String roomType;
  private String company;
  private String companyId;
  private String crud;
  Connection conn;

  public Connection getConnection() throws SQLException {
    Properties connectionProps = new Properties();
    connectionProps.put("user", "root" );//change username/
    connectionProps.put("password", "kramersweety1999"); //change password

    conn = DriverManager.getConnection("jdbc:mysql://"
            + "localhost" + ":" + 3306 + "/" + "cruise" + "?" +
            "useLegacyDatetimeCode=false&serverTimezone=UTC",
        connectionProps);

    return conn;
  }
  /**
   * gets name of all destinations
   * @return ArrayList of all destinations.
   * @throws SQLException
   */
  public ArrayList<String> getDestinations() throws SQLException {
    Connection conn = this.getConnection();
    Statement s = conn.createStatement();
    String select = "Select location FROM ports";
    ResultSet rs = s.executeQuery(select);
    ArrayList<String> dest = new ArrayList<>();
    while(rs.next()) {
      String ch = new String(rs.getString("location"));
      dest.add(ch);
    }
    return dest;
  }

  /**
   * gets name of all company names.
   * @return ArrayList of all company names
   * @throws SQLException
   */
  public ArrayList<String> getCompany() throws SQLException {
    Connection conn = this.getConnection();
    Statement s = conn.createStatement();
    String select = "Select name FROM cruisecompany";
    ResultSet rs = s.executeQuery(select);
    ArrayList<String> comp = new ArrayList<>();
    while(rs.next()) {
      String ch = rs.getString("name");
      comp.add(ch);
    }
    return comp;
  }

  /**
   * asks user to input data needed to find a cruise.
   * Asks and allows user to type desire destination, dates, cruise, and roomtype.
   */
  void begin() throws SQLException {
    System.out.println("Are you a cruise company or customer?");
    Scanner scan = new Scanner(System.in);
    String input = scan.next();
    while((! input.contentEquals("company")) && (! input.contentEquals("customer"))) {
      System.out.println("Please try again, are you a company or customer?");
      scan.reset();
      input = scan.next();
    }
    if (input.equals("customer")) {
      String fname;
      String lname;
      String phnum;
      System.out.println("Choose a destination " + getDestinations().toString());
      destination = scan.next();

      System.out.println("When would you like to leave?");
      startDate = scan.next();

      System.out.println("How many days would you like to stay?");
      endDate = scan.next();

      System.out.println("How many beds do you need?");
      roomType = scan.next();


      //gets all options for a cruise based on given information
      conn = this.getConnection();
      CallableStatement cs = conn.prepareCall("CALL generate_cruise(?,?,?,?)");
      cs.setString(1,destination);
      cs.setString(2, startDate);
      cs.setString(3, endDate);
      cs.setString(4, roomType);
      ResultSet rs = null;
      try {
         rs = cs.executeQuery();
      } catch (Exception e) {
        System.out.println("Please try again");
      }

      assert rs != null;
      ResultSetMetaData rsmd = rs.getMetaData();
      int columnsNumber = rsmd.getColumnCount();
      if(columnsNumber < 1) {
        System.out.println("There were no cruises available matching your "
            + "criteria. Please try again");
      }
      else {
        while (rs.next()) {
          for (int i = 1; i <= columnsNumber; i++) {
            if (i > 1) System.out.print(",  ");
            String columnValue = rs.getString(i);
            System.out.print("One match: "+ columnValue);
          }
          System.out.println();
        }
        cs.close();
        rs.close();
      }



      System.out.println("Would you like to continue with this option?");
      ans = scan.next();
      if(ans.equals("yes") ||ans.equals("Yes") ) {
        System.out.println("Please enter in your information");
        System.out.println("First name: ");
        fname = scan.next();

        System.out.println("Last name: ");
        lname = scan.next();

        System.out.println("Phone number:  ");
        phnum = scan.next();

        CallableStatement call = conn.prepareCall("{? = call getInvoice()}");
        call.registerOutParameter(1, java.sql.Types.VARCHAR);
        call.executeUpdate();

        CallableStatement id = conn.prepareCall("{? = call getpID()}");
        id.registerOutParameter(1, java.sql.Types.VARCHAR);
        id.executeUpdate();

        System.out.println("Your transaction has been complete. Your invoice number is " +
            call.getString(1));

        Random rand = new Random();
        int room = rand.nextInt(1000);

        CallableStatement newId = conn.prepareCall("CALL generate_ID(?,?,?,?)");
        newId.setString(1,destination);
        newId.setString(2, startDate);
        newId.setString(3, endDate);
        newId.setString(4, roomType);
        ResultSet r = null;
        try {
          r = newId.executeQuery();
        } catch (Exception e) {
          System.out.println("Please try again");
        }
        assert r != null;
        r.next();

        //inserts new passenger into passenger table
        Statement stmt = conn.createStatement();
        String sql = "INSERT INTO passengerInfo " +
            "VALUES (" + id.getString(1) + ", " + "'" + fname +"'"+ ", " + "'"+
            lname+"'" + ", "+ "'"+phnum +"'"+ ", "
            +
            r.getString(1) + ", " +
            call.getString(1) + ", " + room + ")";
        stmt.executeUpdate(sql);
      }
    }
    else if (input.equals("company")) {

      //todo me this needs to verify that company is legit
      System.out.println("What company are you with?");
       company = scan.next();

       if(! getCompany().contains(company)) {
         System.out.println("Invalid name company");
       }

      //to do me this needs to verify the ID
      System.out.println("Please enter your staff ID");
      companyId = scan.next();


      System.out.println("Would you like to add or remove a passenger on a cruise?");
      crud = scan.next();
      if(crud.equals("remove")) {
        String pid;
        System.out.println("Enter the passenger id which you wish to delete");
        pid = scan.next();
        String sql = "DELETE FROM passengerinfo WHERE pID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

          pstmt.setString(1, pid);
          pstmt.executeUpdate();

        } catch (SQLException e) {
          System.out.println(e.getMessage());
        }
        System.out.println("Passenger Removed");
      }
      else {
        System.out.println("Please enter information for new passenger ");
        System.out.println("First name: ");
         String first = scan.next();

        System.out.println("Last name: ");
        String last = scan.next();

        System.out.println("Phone number:  ");
        String phone = scan.next();

        System.out.println("Cruise ID:  ");
        String cruiseId = scan.next();

        CallableStatement id = conn.prepareCall("{? = call getpID()}");
        id.registerOutParameter(1, java.sql.Types.VARCHAR);
        id.executeUpdate();


        CallableStatement call = conn.prepareCall("{? = call getInvoice()}");
        call.registerOutParameter(1, java.sql.Types.VARCHAR);
        call.executeUpdate();

        Random rand = new Random();
        int room = rand.nextInt(1000);

        Statement stmt = conn.createStatement();
        String sql = "INSERT INTO passengerInfo " +
            "VALUES (" + id.getString(1) + ", " + "'" + first +"'"+ ", " + "'"+
            last+"'" + ", "+ "'"+phone +"'"+ ", "
            +
            cruiseId + ", " + call.getString(1)
             + ", " + room + ")";
        stmt.executeUpdate(sql);

        System.out.println("Passenger Updated");
      }


    }

  }

  public static void main(String[] args) throws SQLException {
    Frontend app = new Frontend();
    app.begin();
  }

}
