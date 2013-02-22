package  tinyq;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPMultiSenderTrd extends Thread {

  private int port;
  private InetAddress group;
  private UDPFactory udp_factory;
  private LinkedBlockingQueue<UDPMessage> message_list;

  public UDPMultiSenderTrd(LinkedBlockingQueue<UDPMessage> udpMessageList, String groupAddress,
      UDPFactory udpFactory) throws UDPException, UnknownHostException {
    this("Unnamed", udpMessageList, groupAddress, udpFactory);
  }

  public UDPMultiSenderTrd(String threadName, LinkedBlockingQueue<UDPMessage> messageList,
      String groupAddress, UDPFactory udpFactory) throws UDPException, UnknownHostException {
    super(threadName + " group sender thread");
    this.udp_factory  = udpFactory;
    this.message_list = messageList;
    this.port         = udp_factory.getGroupPort();
    this.group        = InetAddress.getByName(groupAddress);
  }

  /* Wait for new message */
  private void doWaiting() throws InterruptedException {
    synchronized (message_list) {
      message_list.wait(); // Wait for payload.
    }
  }

  private DatagramPacket newPacket(byte[] bf, InetAddress grp, int p) {
    return new DatagramPacket(bf, bf.length, grp, p);
  }

  private void doSending(InetAddress group) {
    try {
      byte[] buf = new byte[512];
      UDPMessage aMsg = message_list.take();
      buf = aMsg.toString().getBytes();
      DatagramPacket packet = newPacket(buf, group, port);
      UDPFactory.loop_back_clock();
      udp_factory.getGroupSocket().send(packet); 
      if (message_list.isEmpty()) doWaiting();
    } catch (InterruptedException e) {
      System.out.println(this.getName() + " got interrupted");
      this.interrupt();
      return;
    } catch (SocketException e) {
      System.out.println("Socket closed on " + this.getName());
      this.interrupt(); // Guarantees interruption, returns and exit from run()
      return;
    } catch (Exception e) {
      // this.interrupt(); Continue sending, what-so-ever. (if possible)
      System.err.println("Warning, exception in " + this.getName());
      e.printStackTrace();
    }
  }

  public void run() {
    while (!isInterrupted()) doSending(group);
  }
  
}