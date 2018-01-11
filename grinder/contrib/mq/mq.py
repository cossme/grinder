# A simple example using the MQ plugin that sends a mq message
# and gets a response
#
# 

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from com.ibm.mq import *      
from org.rextency.mq import MQMsg                 
from org.rextency.mq import MQConnector

mqTest = Test(1,"MQSend Test")
mqex = mqTest.wrap(MQConnector())
mqmessage1 = MQMsg()
log = grinder.logger.output

class TestRunner:
    def __call__(self):
    	mqmessage1.setReplyQueue("REPLYQ")
    	mqmessage1.setReplyToQueue("REPLYQ")
    	mqmessage1.setRequestQueue("REQUEST_Q")
    	mqmessage1.setQueueManager("QueueManager")
    	mqmessage1.setReplyToQueueManager("ReplyToQueueManager")
    	mqmessage1.setMessage("MyMessage MyData 010101")	    	
        mqex.init("myserver.com", "9050", "CHANNEL1", mqmessage1)   
        mqex.SendMessage(mqmessage1)
        log(">>>")
        log(mqex.GetMessage(mqmessage1))
        log(">>>")
        mqex.finish() 

