# Java Message Service - Queue Receiver
#
# JMS objects are looked up and messages are created once during
# initialisation. This default JNDI names are for the WebLogic Server
# 7.0 examples domain - change accordingly.
#
# Each worker thread:
#  - Creates a queue session
#  - Receives ten messages
#  - Closes the queue session
#
# This script demonstrates the use of The Grinder statistics API to
# record a "delivery time" custom statistic.
#
# Copyright (C) 2003, 2004, 2005, 2006 Philip Aston
# Copyright (C) 2005 Dietrich Bollmann
# Distributed under the terms of The Grinder license.

from java.lang import System
from java.util import Properties
from javax.jms import MessageListener, Session
from javax.naming import Context, InitialContext
from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from threading import Condition
from weblogic.jndi import WLInitialContextFactory

# Look up connection factory and queue in JNDI.
properties = Properties()
properties[Context.PROVIDER_URL] = "t3://localhost:7001"
properties[Context.INITIAL_CONTEXT_FACTORY] = WLInitialContextFactory.name

initialContext = InitialContext(properties)

connectionFactory = initialContext.lookup("weblogic.examples.jms.QueueConnectionFactory")
queue = initialContext.lookup("weblogic.examples.jms.exampleQueue")
initialContext.close()

# Create a connection.
connection = connectionFactory.createQueueConnection()
connection.start()

# Add two statistics expressions:
# 1. Delivery time:- the mean time taken between the server sending
#    the message and the receiver receiving the message.
# 2. Mean delivery time:- the delivery time averaged over all tests.
# We use the userLong0 statistic to represent the "delivery time".

grinder.statistics.registerDataLogExpression("Delivery time", "userLong0")
grinder.statistics.registerSummaryExpression(
              "Mean delivery time",
                        "(/ userLong0(+ timedTests untimedTests))")

# We record each message receipt against a single test. The
# test time is meaningless.
def recordDeliveryTime(deliveryTime):
    grinder.statistics.forCurrentTest.setValue("userLong0", deliveryTime)

Test(1, "Receive messages").record(recordDeliveryTime)

class TestRunner(MessageListener):

    def __init__(self):
        self.messageQueue = []          # Queue of received messages not yet recorded.
        self.cv = Condition()           # Used to synchronise thread activity.

    def __call__(self):
        log = grinder.logger.info

        log("Creating queue session and a receiver")
        session = connection.createQueueSession(0, Session.AUTO_ACKNOWLEDGE)

        receiver = session.createReceiver(queue)
        receiver.messageListener = self

        # Read 10 messages from the queue.
        for i in range(0, 10):

            # Wait until we have received a message.
            self.cv.acquire()
            while not self.messageQueue: self.cv.wait()
            # Pop delivery time from first message in message queue
            deliveryTime = self.messageQueue.pop(0)
            self.cv.release()

            log("Received message")

            # We record the test a here rather than in onMessage
            # because we must do so from a worker thread.
            recordDeliveryTime(deliveryTime)

        log("Closing queue session")
        session.close()

        # Rather than over complicate things with explict message
        # acknowledgement, we simply discard any additional messages
        # we may have read.
        log("Received %d additional messages" % len(self.messageQueue))

    # Called asynchronously by JMS when a message arrives.
    def onMessage(self, message):
        self.cv.acquire()

        # In WebLogic Server JMS, the JMS timestamp is set by the
        # sender session. All we need to do is ensure our clocks are
        # synchronised...
        deliveryTime = System.currentTimeMillis() - message.getJMSTimestamp()

        self.messageQueue.append(deliveryTime)

        self.cv.notifyAll()
        self.cv.release()
