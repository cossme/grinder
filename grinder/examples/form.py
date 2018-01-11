# HTTP multipart form submission
#
# This script uses the HTTPClient.Codecs class to post itself to the
# server as a multi-part form. Thanks to Marc Gemis.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import Codecs, NVPair
from jarray import zeros

test1 = Test(1, "Upload Image")
request1 = HTTPRequest(url="http://localhost:7001/")
test1.record(request1)

class TestRunner:
    def __call__(self):

        files = ( NVPair("self", "form.py"), )
        parameters = ( NVPair("run number", str(grinder.runNumber)), )

        # This is the Jython way of creating an NVPair[] Java array
        # with one element.
        headers = zeros(1, NVPair)

        # Create a multi-part form encoded byte array.
        data = Codecs.mpFormDataEncode(parameters, files, headers)
        grinder.logger.output("Content type set to %s" % headers[0].value)

        # Call the version of POST that takes a byte array.
        result = request1.POST("/upload", data, headers)
