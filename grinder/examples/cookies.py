# HTTP cookies
#
# HTTP example which shows how to access HTTP cookies.
#
# The HTTPClient library handles cookie interaction and removes the
# cookie headers from responses. If you want to access these cookies,
# one way is to define your own CookiePolicyHandler. This script defines
# a CookiePolicyHandler that simply logs all cookies that are sent or
# received.
#
# The script also demonstrates how to query what cookies are cached for
# the current thread, and how add and remove cookies from the cache.
#
# If you really want direct control over the cookie headers, you
# can disable the automatic cookie handling with:
#    HTTPPluginControl.getConnectionDefaults().useCookies = 0

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest, HTTPPluginControl
from HTTPClient import Cookie, CookieModule, CookiePolicyHandler
from java.util import Date

log = grinder.logger.info

# Set up a cookie handler to log all cookies that are sent and received.
class MyCookiePolicyHandler(CookiePolicyHandler):
    def acceptCookie(self, cookie, request, response):
        log("accept cookie: %s" % cookie)
        return 1

    def sendCookie(self, cookie, request):
        log("send cookie: %s" % cookie)
        return 1

CookieModule.setCookiePolicyHandler(MyCookiePolicyHandler())

test1 = Test(1, "Request resource")
request1 = HTTPRequest()
test1.record(request1)


class TestRunner:
    def __call__(self):
        # The cache of cookies for each  worker thread will be reset at
        # the start of each run.

        result = request1.GET("http://localhost:7001/console/?request1")

        # If the first response set any cookies for the domain,
        # they willl be sent back with this request.
        result2 = request1.GET("http://localhost:7001/console/?request2")

        # Now let's add a new cookie.
        threadContext = HTTPPluginControl.getThreadHTTPClientContext()

        expiryDate = Date()
        expiryDate.year += 10

        cookie = Cookie("key", "value","localhost", "/", expiryDate, 0)

        CookieModule.addCookie(cookie, threadContext)

        result = request1.GET("http://localhost:7001/console/?request3")

        # Get all cookies for the current thread and write them to the log
        cookies = CookieModule.listAllCookies(threadContext)
        for c in cookies: log("retrieved cookie: %s" % c)

        # Remove any cookie that isn't ours.
        for c in cookies:
            if c != cookie: CookieModule.removeCookie(c, threadContext)

        result = request1.GET("http://localhost:7001/console/?request4")

