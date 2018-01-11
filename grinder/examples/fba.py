# HTTP/J2EE form based authentication
#
# A more complex HTTP example based on an authentication conversation
# with the server. This script demonstrates how to follow different
# paths based on a response returned by the server and how to post
# HTTP form data to a server.
#
# The J2EE Servlet specification defines a common model for form based
# authentication. When unauthenticated users try to access a protected
# resource, they are challenged with a logon page. The logon page
# contains a form that POSTs username and password fields to a special
# j_security_check page.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import NVPair

protectedResourceTest = Test(1, "Request resource")
authenticationTest = Test(2, "POST to j_security_check")

request = HTTPRequest(url="http://localhost:7001/console")
protectedResourceTest.record(request)

class TestRunner:
    def __call__(self):
        result = request.GET()
        result = maybeAuthenticate(result)

        result = request.GET()

# Function that checks the passed HTTPResult to see whether
# authentication is necessary. If it is, perform the authentication
# and record performance information against Test 2.
def maybeAuthenticate(lastResult):
    if lastResult.statusCode == 401 \
    or lastResult.text.find("j_security_check") != -1:

        grinder.logger.info("Challenged, authenticating")

        authenticationFormData = ( NVPair("j_username", "weblogic"),
                                   NVPair("j_password", "weblogic"),)

        request = HTTPRequest(url="%s/j_security_check" % lastResult.originalURI)
        authenticationTest.record(request)

        return request.POST(authenticationFormData)
