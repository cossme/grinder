# Simple HTTP Web Service
#
# Calls an Amazon.com web service to obtain information about a book.
#
# To run this script you must install the standard Python xml module.
# Here's one way to do that:
#
#   1. Download and install Jython 2.1
#   2. Add the following line to grinder.properties (changing the path appropriately):
#           grinder.jvm.arguments=-Dpython.home=c:/jython-2.1
#   3. Add Jakarta Xerces (or one of the other parsers supported by
#       the xml module) to your CLASSPATH.
#
# You may also need to obtain your own Amazon.com web service license
# and replace the script text <insert license key here> with the
# license key, although currently that doesn't appear to be necessary.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import NVPair
from xml.dom import javadom
from org.xml.sax import InputSource

bookDetailsTest = Test(1, "Get book details from Amazon")
parser = javadom.XercesDomImplementation()

class TestRunner:
    def __call__(self):
        if grinder.runNumber > 0 or grinder.threadNumber > 0:
            raise RuntimeError("Use limited to one thread, one run; "
                               "see Amazon Web Services terms and conditions")

        request = HTTPRequest(url="http://xml.amazon.com/onca/xml")
        bookDetailsTest.record(request)

        parameters = (
            NVPair("v", "1.0"),
            NVPair("f", "xml"),
            NVPair("t", "webservices-20"),
            NVPair("dev-t", "<insert license key here>"),
            NVPair("type", "heavy"),
            NVPair("AsinSearch", "1904284000"),
            )

        bytes = request.POST(parameters).inputStream

        # Parse results
        document = parser.buildDocumentUrl(InputSource(bytes))

        result = {}

        for details in document.getElementsByTagName("Details"):
            for detailName in ("ProductName", "SalesRank", "ListPrice"):
                result[detailName] = details.getElementsByTagName(
                    detailName)[0].firstChild.nodeValue

        grinder.logger.info(str(result))

