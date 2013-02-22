package  tinyq;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Receiver thread to enqueue UDP messages.
 * @author Davis - 5715822
 */
public class UDPUniRecverTrd extends Thread  {
  
  private UDPFactory udp_factory;
  private LinkedBlockingQueue<UDPMessage> recev_queue;

  public UDPUniRecverTrd(LinkedBlockingQueue<UDPMessage> recvQueue, DatagramSocket socket) 
      throws UDPException {
    this("Unnamed", recvQueue, socket);
  }
  
  public UDPUniRecverTrd(String threadName, LinkedBlockingQueue<UDPMessage>recvQueue,
      DatagramSocket socket) throws UDPException {
    super(threadName + " unicast receiver thread");
    this.recev_queue = recvQueue;
    this.udp_factory = new UDPFactory();
    this.udp_factory.setUnicastSocket(socket);
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
      long    desirAmt = Long.parseLong(sdesiamt);
      return new UDPMessage(show, custID, cmd, tickets, transac, option, desirShw, desirAmt);
  }
  
  private void doReceiving() {
    byte[] buf = new byte[512];
    
    try {
      DatagramPacket packet = new DatagramPacket(buf, buf.length);
      udp_factory.getUnicastSocket().receive(packet); 
      String msg[] = new String(packet.getData(), 0, packet.getLength()).split("\n");
      UDPMessage udpMessage = decodeMessage(msg);
      udpMessage.recv_from_ip = (packet.getAddress().toString()).replace("/", "");
      udpMessage.recv_from_port = packet.getPort();
      recev_queue.add(udpMessage);
    } catch (SocketException e) { 
      System.out.println("Socket closed on " + this.getName());
      this.interrupt(); // Guarantees interruption, returns and exit from run()
      return;
    } catch (Exception e) {
      System.out.println(e.getMessage() + " in " + this.getName());
      // e.printStackTrace();
      // this.interrupt(); Continue receiving, what-so-ever. (if possible)
    }
  }

  public void run() {
    while (!isInterrupted()) doReceiving();
  }

  public void closeSock() {
    udp_factory.getUnicastSocket().close();
  }
}