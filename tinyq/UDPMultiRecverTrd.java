package  tinyq;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Receiver thread to enqueue UDP messages.
 * @author Davis - 5715822
 */
public class UDPMultiRecverTrd  extends Thread {
  
  private UDPFactory udp_factory;
  private LinkedBlockingQueue<UDPMessage> message_list;

  public UDPMultiRecverTrd(LinkedBlockingQueue<UDPMessage> messageList, String groupAddress,
      UDPFactory udpFactory) throws UDPException {
    this("Unnamed", messageList, groupAddress, udpFactory);
  }
  
  public UDPMultiRecverTrd(String threadName, LinkedBlockingQueue<UDPMessage>messageList,
      String groupAddress, UDPFactory udpFactory) throws UDPException {
    super(threadName + " group receiver thread");
    this.udp_factory  = udpFactory;
    this.message_list = messageList;
  }

  private UDPMessage decodeMessage(String[] msg) {
      String  command  = msg[0].split(":")[1].trim();
      String  transID  = msg[1].split(":")[1].trim();
      String  custID   = msg[2].split(":")[1].trim();
      String  show     = msg[3].split(":")[1].trim();
      String  stickets = msg[4].split(":")[1].trim();
      String  option   = msg[5].split(":")[1].trim();
      String  desirShw = msg[6].split(":")[1].trim();
      String  sdesiamt = msg[7].split(":")[1].trim();
      Command cmd      = Command.valueOf(command);
      long    transac  = Long.parseLong(transID);
      long    tickets  = Long.parseLong(stickets);
      Long    desirAmt = Long.parseLong(sdesiamt);
      return new UDPMessage(show, custID, cmd, tickets, transac, option, desirShw, desirAmt);
  }
  
  private void doReceiving() {
    byte[] buf = new byte[512];
    DatagramPacket packet = new DatagramPacket(buf, buf.length);
    try {
      udp_factory.getGroupSocket().receive(packet);
      String sBuf = new String(packet.getData(), 0, packet.getLength());
      UDPMessage udpMessage = decodeMessage(sBuf.split("\n"));
      udpMessage.recv_from_ip = (packet.getAddress().toString()).replace("/", "");
      udpMessage.recv_from_port = packet.getPort();
      message_list.add(udpMessage);
    } catch (SocketException e) {
      this.interrupt();
      System.out.println("Socket closed on " + this.getName());
      return;
    } catch (Exception e) {
      // this.interrupt(); Continue receiving, what-so-ever. (if possible)
      System.out.println("Warning, exception in " + this.getName());
      e.printStackTrace();
    }
  }

  public void run() {
    while (!isInterrupted()) doReceiving();
  }
}
