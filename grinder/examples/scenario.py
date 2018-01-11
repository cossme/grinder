# Recording many HTTP interactions as one test
#
# This example shows how many HTTP interactions can be grouped as a
# single test by wrapping them in a function.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import NVPair

# We declare a default URL for the HTTPRequest.
request = HTTPRequest(url = "http://localhost:7001")

def page1():
    request.GET('/console')
    request.GET('/console/login/LoginForm.jsp')
    request.GET('/console/login/bea_logo.gif')

Test(1, "First page").record(page1)

class TestRunner:
    def __call__(self):
        page1()
