package org.rextency.mq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import com.ibm.mq.*;
import com.ibm.mq.MQPutMessageOptions;
import org.rextency.mq.MQMsg;
/**
 * An MQ Class, for sending MQ messages
 *
 * @author Phillip Mayhew
 * @version $Revision$
 */
public class MQConnector {


  private static Pattern s_pathParser =
    Pattern.compile("([^?#]*)(\\?([^#]*))?(#(.*))?");

  private MQQueueManager qmgr;
  private MQQueue squ;
  private MQQueue rqu;
  private int GMO_Options = MQC.MQGMO_WAIT | MQC.MQMO_MATCH_CORREL_ID;
  private int GMO_WaitInterval = 120000;

  public MQConnector() 
  {

  }

  public boolean init(String pHostname, String pPort, String pChannel, MQMsg msg)
  {
  	  MQEnvironment.hostname = pHostname;
  	  MQEnvironment.port = Integer.parseInt(pPort);
  	  MQEnvironment.channel = pChannel;
	  MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY,MQC.TRANSPORT_MQSERIES);

  	try
  	{
      System.out.println("QueueManager: " + msg.getQueueManager());
  	  System.out.println("RequestQueue: " + msg.getRequestQueue());
      System.out.println("ReplyQueue: " + msg.getReplyQueue());
            
  	  if((msg.getQueueManager()).length() > 0)
  	     qmgr = new MQQueueManager(msg.getQueueManager(), MQC.MQCNO_FASTPATH_BINDING);
  	  else
  	  	  return false;
  	  
  	  if((msg.getRequestQueue()).length() > 0)
  	     squ = qmgr.accessQueue(msg.getRequestQueue(),msg.getOpenOptions());

  	  if((msg.getReplyQueue()).length() > 0)
  	     rqu = qmgr.accessQueue(msg.getReplyQueue(),msg.getReadOptions());
  	 
  	} 
  	
  	catch(Exception e)
  	{ System.out.println("Error in: MQConnector.init(): " + e.getMessage()); }
  	
  	return true;
  }
  public void SendMessage(MQMsg msg)
  {
  	  System.out.println("ReplyToQueue: " + msg.getReplyToQueue());
      System.out.println("ReplyToQueueManager: " + msg.getReplyToQueueManager());
  	try
  	{
    	MQPutMessageOptions pmo = new MQPutMessageOptions();
    	MQMessage rqMessage = new MQMessage();
    	
    	rqMessage.format = MQC.MQFMT_STRING;
    	rqMessage.replyToQueueName = msg.getReplyToQueue();
    	rqMessage.replyToQueueManagerName = msg.getReplyToQueueManager();
    	rqMessage.messageType = MQC.MQMT_REQUEST;
    	rqMessage.persistence = MQC.MQPER_NOT_PERSISTENT;
    	
    	pmo.options = MQC.MQPMO_NEW_MSG_ID;
    	
    	rqMessage.writeString(msg.getMessage());
    	squ.put(rqMessage, pmo);
    	
    	msg.setCorrelationID(new String(rqMessage.messageId));
    	squ.close();
  	}
  	catch(Exception e)
  	{ System.out.println("Exception occured during MQConnector.SendMessage(): " + e.getMessage()); }
  }
  
  public String GetMessage(MQMsg msg)
  {
  	 String retVal = new String();
  	 try
  	 {
  	  MQGetMessageOptions gmo = new MQGetMessageOptions();
  	  gmo.options = this.GMO_Options;
  	  gmo.waitInterval = this.GMO_WaitInterval;
  	  
  	  MQMessage rtMessage = new MQMessage();
  	  rtMessage.correlationId = (msg.getCorrelationID()).getBytes();
  	  System.out.println("CorrelationID: " + byteArrayToHexString((msg.getCorrelationID()).getBytes()));
  	  rqu.get(rtMessage, gmo);  	  
  	  retVal = rtMessage.readString(rtMessage.getMessageLength());
  	  rqu.close();
  	 }
  	catch(Exception e)
	  	{ System.out.println("Exception occured during MQConnector.GetMessage(): " + e.getMessage()); }
  	 
  	  return retVal; 	  
  }

  public void finish()
  {
  	 try
  	 {
  	   	  qmgr.disconnect();
  	 }
  	catch(Exception e)
	  	{ System.out.println("Exception occured during MQConnector.finish(): " + e.getMessage()); }
  	 
  }
  public String toString() {
    final StringBuffer result = new StringBuffer("MQMessage");

    return result.toString();
  }

  private String byteArrayToHexString(byte in[]) 
  {
    byte ch = 0x00;
    int i = 0; 

    if (in == null || in.length <= 0)
        return null;
     
    String pseudo[] = {"0", "1", "2","3","4", "5", "6", "7", 
					   "8", "9", "A", "B", "C", "D", "E","F"};

    StringBuffer out = new StringBuffer(in.length * 2);  
    
    while (i < in.length) 
    {
       ch = (byte) (in[i] & 0xF0); // Strip off high nibble
       ch = (byte) (ch >>> 4);     // shift the bits down
       ch = (byte) (ch & 0x0F);    // must do this is high order bit is on!
       out.append(pseudo[ (int) ch]); // convert the nibble to a String Character
       ch = (byte) (in[i] & 0x0F); // Strip off low nibble 
       out.append(pseudo[ (int) ch]); // convert the nibble to a String Character
       i++;
    }

    String rslt = new String(out);
    return rslt;
  } 
}
