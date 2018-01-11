# Simple HTTP example
#
# A simple example using the HTTP plugin that shows the retrieval of a
# single page via HTTP. The resulting page is written to a file.
#
# More complex HTTP scripts are best created with the TCPProxy.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest

test1 = Test(1, "Request resource")
request1 = HTTPRequest()
test1.record(request1)

class TestRunner:
    def __call__(self):
        result = request1.GET("http://localhost:7001/")

        # result is a HTTPClient.HTTPResult. We get the message body
        # using the getText() method.
        writeToFile(result.text)

# Utility method that writes the given string to a uniquely named file.
def writeToFile(text):
    filename = "%s-page-%d.html" % (grinder.processName, grinder.runNumber)

    file = open(filename, "w")
    print >> file, text
    file.close()
