package  tinyq;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Sender thread to dispatch UDP messages.
 * @author Davis - 5715822
 */
public class UDPUniSenderTrd extends Thread {
  private UDPFactory udp_factory;
  private LinkedBlockingQueue<UDPMessage> message_list;

  public UDPUniSenderTrd(LinkedBlockingQueue<UDPMessage> udpMessageList, DatagramSocket socket) 
      throws UDPException, UnknownHostException {
    this("Unnamed", udpMessageList, socket);
  }

  public UDPUniSenderTrd(String threadName, LinkedBlockingQueue<UDPMessage> messageList, DatagramSocket socket) 
      throws UDPException, UnknownHostException {
    super(threadName + " unicast sender thread");
    this.udp_factory  = new UDPFactory();
    this.message_list = messageList;
    udp_factory.setUnicastSocket(socket);
  }

  private DatagramPacket newPacket(byte[] bf, InetAddress destIP, int send_to_port) {
    return new DatagramPacket(bf, bf.length, destIP, send_to_port);
  }

  private void doSending() {
    try {
      byte[] buf = new byte[512];
      UDPFactory.loop_back_clock();
      UDPMessage aMsg = message_list.take();// Will wait() if no message.
      buf = aMsg.toString().getBytes();
      InetAddress sendToIP = InetAddress.getByName(aMsg.send_to_ip); 
      DatagramPacket packet = newPacket(buf, sendToIP, aMsg.send_to_port);
      udp_factory.getUnicastSocket().send(packet);
    } catch (InterruptedException e) {
      System.out.println(this.getName() + " got interrupted"); 
      this.interrupt(); // Guarantees interruption, returns and exit from run()
      return;
    } catch (Exception e) {
      System.out.println(e.getMessage() + " in " + this.getName());
      // e.printStackTrace();
      // this.interrupt();   Continue sending, what-so-ever (if possible).
    }
  }

  public void run() {
    while (!isInterrupted()) doSending();
  }
  
  public void closeSoket() {
    udp_factory.getUnicastSocket().close();
  }
}