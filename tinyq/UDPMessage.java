package  tinyq;

/**
 * @author Davis Desormeaux
 * 
 * The Message object containing what is to be sent
 * or received over UDP.
 *
 */
public class UDPMessage {
  
  public Command    command;
  public String     show_id;
  public String     desired_show_id;
  public String     op_text;
  public String     cust_id;
  public long       tickets;
  public long       desired_tickets;
  public long       transac;
  public String     recv_from_ip;
  public int        recv_from_port;
  protected String  send_to_ip;
  protected int     send_to_port;

  
  public UDPMessage(){
      this.tickets = 0;
      this.transac = 0;
      this.desired_tickets = 0;
  }
  
  public UDPMessage(String showID, String custID, Command command, long tickets, long transacID, String opText) {
    this.command = command;
    this.tickets = tickets;
    this.transac = transacID;
    this.cust_id = custID;
    this.op_text = opText;
    this.show_id = showID;
    this.desired_tickets = 0;
    this.desired_show_id = "";
  }
  
  public UDPMessage(String showID, String custID, Command command, 
      long tickets, long transacID, String opText, String desiredShow, long desiredAmt) {
    this.command = command;
    this.tickets = tickets;
    this.transac = transacID;
    this.cust_id = custID;
    this.op_text = opText;
    this.show_id = showID;
    this.desired_tickets = desiredAmt;
    this.desired_show_id = desiredShow; 
  }

  public String toString(){
    butEscapeInvalidChar();
    String msgContent = 
        "Command           : " + command         + "\n" +
        "Transction #      : " + transac         + "\n" +
        "Customer ID       : " + cust_id         + "\n" +
        "Show ID           : " + show_id         + "\n" +
        "Ticket Amt        : " + tickets         + "\n" +
        "Optional Text     : " + op_text         + "\n" + 
        "Desired Show ID   : " + desired_show_id + "\n" +
        "Desired Tick AMT  : " + desired_tickets + "\n" +
        "Sender's IP       : " + recv_from_ip    + "\n" + 
        "Sender's Port     : " + recv_from_port  + "\n" ;
    return msgContent;
  }
  
  private void butEscapeInvalidChar(){
    cust_id = (cust_id == null) ? "" : (cust_id.replace(":", "->")).replace("\n", "");
    op_text = (op_text == null) ? "" : (op_text.replace(":", "->")).replace("\n", "");
    show_id = (show_id == null) ? "" : (show_id.replace(":", "->")).replace("\n", "");
    desired_show_id = (desired_show_id == null) ? "" : 
      (desired_show_id.replace(":", "->")).replace("\n", "");
  }
}

