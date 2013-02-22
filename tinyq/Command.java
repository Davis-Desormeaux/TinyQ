package tinyq;

/**
 * @author Davis Desormeaux - 5715822
 */
public enum Command {
  
  /* DTRS Commands */
  CHECK      (10), 
  RESERVE    (11), 
  CANCEL     (12), 
  EXCHANGE   (14), /* Skip 13... */
  CANEXCHANGE(15), /* Is sent to another leader. He'll Execute or gives ERROR. */ 
  NEW_LEADER (19), /* Leader use this one to broadcast his IP Address.
  
  /* GMS Commands */
  WHO_LEADS  (20), /* GMS use it to inquire about a group's leader IP Address.  */
  OTHER_LEAD (30), /* IP of the leader in the other group. Needed for exchange. */
  WAKEUP     (40), /* To wake a passive node. */
  BPING      (50), /* Broadcast ping, should be used with sendToGroup() */
  PONG       (60), /* Is not really a GMS, but look good below PING */ 
  
  /* Reply Code */
  SUCCESS    (200),
  ERROR      (404),
  
  /* Admin/Test command */
  SHUTDOWN   (999);
  
  private int value;

  Command(int value) {
    this.value = value;
  }
  
  public int getValue(){
    return this.value;
  }
};  