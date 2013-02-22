package tinyq;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Helper class to handle UDP messaging.
 * @author Davis Desormeaux - 5715822
 */
public class UDPHelper {
    
  private String group_address;
  private boolean isBroadcaster;
  private UDPFactory udp_grp_factry;
  private UDPUniSenderTrd udp_uni_sender_trd; //   Thread for sending unicast message
  private UDPUniRecverTrd udp_uni_recver_trd; // Thread for receiving unicast message
  private UDPMultiRecverTrd udp_group_recver_trd; //   Thread for receiving multicast    
  private UDPMultiSenderTrd udp_group_sender_trd; //   Thread for sending   multicast 
  private LinkedBlockingQueue<UDPMessage> uni_recv_queue; //  Unicast recv FIFO Queue                    
  private LinkedBlockingQueue<UDPMessage> uni_send_queue; //  Unicast send FIFO Queue
  private LinkedBlockingQueue<UDPMessage> multi_msg_queue; //    Multicast FIFO Queue   
  
  /* UDPHelper Constructor */
  public UDPHelper() {}

   /**
   * @param nodeID The nodeID or id to identify your server
   * @param port The port number you wish to use.
   * @throws UDPException 
   */
  public void startUnicastServer(String nodeID, int port) throws UDPException {
    try {
      UDPFactory unicastFactory = new UDPFactory();
      if (port == 0) unicastFactory.createUnicastSocket();
      else unicastFactory.createUnicastSocket(port);
      uni_recv_queue = new LinkedBlockingQueue<UDPMessage>();
      uni_send_queue = new LinkedBlockingQueue<UDPMessage>();
      DatagramSocket socket = unicastFactory.getUnicastSocket();
      udp_uni_sender_trd = new UDPUniSenderTrd(nodeID, uni_send_queue, socket);
      udp_uni_recver_trd = new UDPUniRecverTrd(nodeID, uni_recv_queue, socket);
      udp_uni_recver_trd.start(); // Unicast thread to get messages.
      udp_uni_sender_trd.start(); // Unicast thread to send message.
    } catch (UnknownHostException e) {
      throw new UDPException(e);
    }
  }
  
  /**
  * @param nodeID The nodeID or id to identify your client
  * @throws UDPException
  */
  public void startUnicastClient(String nodeID) throws UDPException {
    uni_recv_queue = new LinkedBlockingQueue<UDPMessage>();
    uni_send_queue = new LinkedBlockingQueue<UDPMessage>();
    startUnicastServer(nodeID, 0); // A server but with no specific port.
  }
  
  /**
  *  Close all unicast ressources (socket, threads, and queues)
   * @throws UDPException 
  */
  public void stopUnicast() throws UDPException {
    try {  
      while (!uni_send_queue.isEmpty()) {
        try { 
          Thread.sleep(10); // Wait for deQueue...
        } catch (InterruptedException e) { 
          System.err.println(e.getStackTrace()); 
        } 
      }
      udp_uni_sender_trd.interrupt();
      udp_uni_sender_trd.join();
      udp_uni_recver_trd.closeSock();
      udp_uni_recver_trd.join();
    } catch (InterruptedException e) {
      throw new UDPException(e);
    }
  }
  
  /* End the appropriate Multicast thread */
  private void endThread(int type) throws Exception {
    switch (type) {
    case 0: // Replica, Duplicate or Passive
      udp_group_recver_trd.interrupt();
      udp_grp_factry.getGroupSocket().leaveGroup(InetAddress.getByName(group_address));
      udp_grp_factry.closeGroupSocket(); 
      udp_group_recver_trd.join();
      break;

    case 1: // Leader
      udp_grp_factry.getGroupSocket().leaveGroup(InetAddress.getByName(group_address));
      udp_grp_factry.closeGroupSocket(); 
      udp_group_sender_trd.interrupt();
      udp_group_sender_trd.join();
      break;

    default:
      throw new UDPException("Invalid type + (" + type + ")", null);
    }
  }
  /* Start the appropriate Multicast thread */
  private void startThread(int type, String nID) throws UDPException, UnknownHostException {
    switch (type) {
    case 0: // Replica, Duplicate or Passive
      udp_group_recver_trd = new UDPMultiRecverTrd(nID, multi_msg_queue, group_address, udp_grp_factry);
      udp_group_recver_trd.start();
      break;

    case 1: // Leader
      udp_group_sender_trd = new UDPMultiSenderTrd(nID, multi_msg_queue, group_address, udp_grp_factry);
      udp_group_sender_trd.start(); // Leaders need this thread to broadcast.
      break;

    default:
      throw new UDPException("Invalid type + (" + type + ")", null);
    }
  }


  /**
   * Join a group (connect to), if group doesn't exist, a new one will be created.
   * @param groupAddress The multicast address of the group.
   * @throws UDPException Thrown if group cannot be joined.
   */
  public void joinGroup(String groupAdr, int grpPort, boolean broadcast, String nID) throws UDPException {
    try {
      this.udp_grp_factry = new UDPFactory(); 
      this.isBroadcaster = broadcast; //Brodcast will try to become the lead
      this.group_address = groupAdr;
      this.multi_msg_queue = new LinkedBlockingQueue<UDPMessage>();
      this.udp_grp_factry.createGroupSocket(grpPort);
      this.udp_grp_factry.getGroupSocket().joinGroup(InetAddress.getByName(group_address));
      startThread(broadcast ? 1 : 0, nID);
    } catch (Exception e) {
      throw new UDPException("Cannot join group address " + groupAdr, e);
    }
  }

  /**
   * Leave the group (disconnect)
   * @exception UDPException Thrown if an error occurred.
   */
  public void leaveGroup() throws UDPException {
    try {
      while (!multi_msg_queue.isEmpty() && isBroadcaster) {
        try { 
          Thread.sleep(10); // Wait for deQueue...
        } catch (InterruptedException e) { 
          System.err.println(e.getStackTrace()); 
        } 
      }
      endThread(isBroadcaster ? 1 : 0);      
    } catch (Exception e) {
      throw new UDPException(e.getMessage() + " while leaving", e);
    }
  }
  
  /**
   * Enqueue a command (check, reserve, etc...) to be send to
   * <code>sentToIP</code>.
   * @param sentToIP IP Address of the destination host.
   * @param sentToPort Port of the destination port.
   * @param UDPMessage Object containing the message to deliver.
   * @throws UDPException thrown if error while delivering the message.
   */
  public void sendToNode(String sentToIP, int sentToPort, UDPMessage message) {
    UDPFactory.loop_back_clock();        // Can't use synchronized(queue) on queue.take()  
    synchronized (uni_send_queue) {      // sender thread. If the clock above is not used,
      message.send_to_ip = sentToIP;     // port and ip gets overwriten when sending to 
      message.send_to_port = sentToPort; // different hosts in a loop. Clock set in UDPFactoy.
      uni_send_queue.add(message);    
      uni_send_queue.notify();
    }
  }

  /**
   * Enqueue a command (check, reserve, etc...) to be broadcasted to
   * <code>groupName</code>.
   * 
   * @param UDPMessage Object containing the message to deliver.
   * @throws UDPException thrown if error while delivering the message.
   */
  public void sendToGroup(UDPMessage message) throws UDPException {
    if (! this.isBroadcaster ) {
        System.err.println("Warning, replica are not authorized to broadcast");
        return;
    } 
    UDPFactory.loop_back_clock();       // Can't use synchronized(queue) on queue.take().
    synchronized (multi_msg_queue) {    // Backtick(s) seems to let the JVM a chance to
      multi_msg_queue.add(message);     // updates her memory. Would happen when someone
      multi_msg_queue.notify();         // would change a UDPMessage by changing the public vars.
    }                                   // ... Thus, not making uses of the 'New' keyword...
  }

  /**
   * Return the group address for this node, must be connected to a group.
   * @return Group name or null if none.
   */
  public String getGroupAddress() {
    return this.group_address;
  }

  /**
   * Get new messages from your group.
   * 
   * @return UDPMessage A new message from your Group or null interrupted
   * @throws UDPException
   */
  public UDPMessage getMsgFromGroup() throws UDPException {
    try {
      synchronized (multi_msg_queue) {
        return multi_msg_queue.take(); // Will wait() if no message.
      }
    } catch (InterruptedException e) {
      System.out.println("Group receieving thread disconnected.");
      return null;
    } catch (Exception e) {
      throw new UDPException("Exceptional exception", e);
    }
  }


  /**
   * Get new messages from your group.
   * @return UDPMessage A new message from your group or null if timeout.
   * @throws UDPException
   */
  public UDPMessage getMsgFromGroupOrTimeout(int timeoutInSeconds) throws UDPException {
    try {
      synchronized (multi_msg_queue) {
        return multi_msg_queue.poll(timeoutInSeconds, TimeUnit.SECONDS); // Will timeout if no message.
      }
    } catch (InterruptedException e) {
      System.out.println("Group receieving thread disconnected.");
      return null;
    } catch (Exception e) {
      throw new UDPException("Exceptional exception", e);
    }
  }
  
  /**
   * @return A Message from UDP Unicast
   * @throws UDPException
   */
  public UDPMessage getMessage() throws UDPException {
    try {
      synchronized (uni_recv_queue) {
        return uni_recv_queue.take(); // Will wait() if no message.
      }
    } catch (InterruptedException e) {
      stopUnicast();
    } catch (Exception e) {
      throw new UDPException("Exceptional exception", e);
    }
    return null;
  }
  
  /**
   * @return A Message from UDP Unicast or null if timeout
   * @throws UDPException
   */
  public UDPMessage getMessageOrTimeout(int timeoutInSeconds) throws UDPException {
    try {
      synchronized (uni_recv_queue) {
        return uni_recv_queue.poll(timeoutInSeconds, TimeUnit.SECONDS); // Will timeout if no message.
      }
    } catch (InterruptedException e) {
      stopUnicast();
    } catch (Exception e) {
      throw new UDPException("Exceptional exception", e);
    }
    return null;
  }

}
