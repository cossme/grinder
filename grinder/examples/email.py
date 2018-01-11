# Email
#
# Send email using Java Mail (http://java.sun.com/products/javamail/)
#
# This Grinder Jython script should only be used for legal email test
# traffic generation within a lab testbed environment. Anyone using
# this script to generate SPAM or other unwanted email traffic is
# violating the law and should be exiled to a very bad place for a
# very long time.
#
# Copyright (C) 2004 Tom Pittard
# Copyright (C) 2004-2008 Philip Aston
# Distributed under the terms of The Grinder license.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test

from java.lang import System
from javax.mail import Message, Session
from javax.mail.internet import InternetAddress, MimeMessage


emailSendTest1 = Test(1, "Email Send Engine")

class TestRunner:
    def __call__(self):
        smtpHost = "mailhost"

        properties = System.getProperties()
        properties["mail.smtp.host"] = smtpHost
        session = Session.getInstance(System.getProperties())
        session.debug = 1

        message = MimeMessage(session)
        message.setFrom(InternetAddress("TheGrinder@yourtestdomain.net"))
        message.addRecipient(Message.RecipientType.TO,
                             InternetAddress("you@yourtestdomain.net"))
        message.subject = "Test email %s from thread %s" % (grinder.runNumber,
                                                            grinder.threadNumber)

        # One could vary this by pointing to various files for content
        message.setText("SMTPTransport Email works from The Grinder!")

        transport = session.getTransport("smtp")

        # Instrument transport object.
        emailSendTest1.record(transport)

        transport.connect(smtpHost, "username", "password")
        transport.sendMessage(message,
                              message.getRecipients(Message.RecipientType.TO))
        transport.close()
