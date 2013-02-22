package tinyq;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketException;

/**
 * Handle mangement of UDP Sockets
 * @author Davis Desormeaux - 5716822
 */
public class UDPFactory {
  protected MulticastSocket multi_socket;
  protected DatagramSocket  uni_socket;
  private int               group_port;
  private int               port;
  final static int          lbc = 10; /* lbc = loopback clock value. Set to 10ms.
                                         Used to synch sending/receiving over 
                                         the 'local loopback interface'.  
                                      */
                                      
  public void createGroupSocket(int groupPort) throws UDPException {
    this.group_port = groupPort;
    try {
      multi_socket = new MulticastSocket(group_port);
    } catch (IOException e) {
      throw new UDPException(e);
    }
  }
  
  /* Unicast socket binded to specefic port. For Leader, FE, and GMS */
  public void createUnicastSocket(int port) throws UDPException {
    this.port = port;
    try {
      uni_socket = new DatagramSocket(port);
    } catch (SocketException e) {
      throw new UDPException(e);
    }
  }
  
  /* Unicast socket */
  public void createUnicastSocket() throws UDPException {
    try {
      uni_socket = new DatagramSocket();
    } catch (SocketException e) {
      throw new UDPException(e);
    }
  }
  
  public void setUnicastSocket(DatagramSocket uniSocket) {
    uni_socket = uniSocket;
  }
  
  public void closeGroupSocket() {
    this.multi_socket.close();

  }

  public void closeUnicastSocket() {
    this.uni_socket.close();
  }

  public MulticastSocket getGroupSocket() {
    return multi_socket;
  }

  public DatagramSocket getUnicastSocket() {
    return uni_socket;
  }

  public int getGroupPort() {
    return group_port;
  }

  public int getUnicastPort() {
    return port;
  }
  
  public static void loop_back_clock(){
      try{Thread.sleep(lbc);}
      catch (InterruptedException e){}
  }
}
