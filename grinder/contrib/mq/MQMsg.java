package org.rextency.mq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.mq.*;

/**
 * An MQ message class
 *
 * @author Phillip Mayhew
 * @version $Revision$
 */
public class MQMsg {

  private static Pattern s_pathParser =
    Pattern.compile("([^?#]*)(\\?([^#]*))?(#(.*))?");


  private String ReplyQueue;
  private String ReplyToQueue;
  private String RequestQueue;
  private String QueueManager;
  private String ReplyToQueueManager;
  private String Message;
  private String CorrelationID;
  
  private int ReadOptions = MQC.MQOO_INPUT_AS_Q_DEF;
  private int OpenOptions = MQC.MQOO_OUTPUT;
  
  public MQMsg() 
  {
  }
  
  public void setCorrelationID(String pCorrelation_ID)
  { this.CorrelationID = pCorrelation_ID; }
  
  public String getCorrelationID()
  { return this.CorrelationID; }
  
  public void setReadOptions(int pRead_Options)
  { this.ReadOptions = pRead_Options; }

  public void setOpenOptions(int pOpen_Options)
  { this.OpenOptions = pOpen_Options; }
  
  public int getReadOptions()
  { return this.ReadOptions; }

  public int getOpenOptions()
  { return this.OpenOptions; }
  
  public void setReplyQueue(String pReply_Queue)
  { this.ReplyQueue = pReply_Queue; }

  public void setReplyToQueue(String pReply_To_Queue)
  { this.ReplyToQueue = pReply_To_Queue; }

  public void setReplyToQueueManager(String pReply_To_Queue_Manager)
  { this.ReplyToQueueManager = pReply_To_Queue_Manager; }
  
  public void setRequestQueue(String pRequest_Queue)
  { this.RequestQueue = pRequest_Queue; }

  public void setQueueManager(String pQueue_Manager)
  { this.QueueManager = pQueue_Manager; }

  public void setMessage(String pMessage)
  { this.Message = pMessage; }

  public String getReplyQueue()
  { return this.ReplyQueue;}

  public String getReplyToQueue()
  { return this.ReplyToQueue;}  	  

  public String getReplyToQueueManager()
  { return this.ReplyToQueueManager;}  
    
  public String getRequestQueue()
  { return this.RequestQueue;}  	  
  
  public String getQueueManager()
  { return this.QueueManager;}
  
  public String getMessage()
  { return this.Message;}

  public String toString() {
    final StringBuffer result = new StringBuffer("MQMessage");

    return result.toString();
  }

}
