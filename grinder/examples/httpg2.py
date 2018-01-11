# Script for The Grinder 3 that runs Grinder 2 HTTP test scripts.

# Copy http-g2.py to the same directory as an existing Grinder 2
# grinder.properties. Add "grinder.script=http-g2.py" to the
# properties file, update the name of the "grinder.cycles" property to
# "grinder.runs", "grinder.thread.sleepTimeFactor" to
# "grinder.sleepTime" and so on. Then use The Grinder 3 to execute the
# script.

# Some caveats: This script has not been fully tested. I'm not
# interested in reproducing every nuance of The Grinder 2 HTTP plugin.
# The script is complex - normal HTTP scripts for The Grinder 3 are
# much simpler. Nethertheless it may be useful for those moving from
# The Grinder 2 to The Grinder 3.

# The following Grinder 2 parameters/features are not supported:
#   useHTTPClient                              (always true)
#   useCookiesVersionString
#   String beans


# TODO:
# What to do about errors
# Control M's in logged data
# Timing vs G2


from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPPluginControl, HTTPRequest
from HTTPClient import NVPair

pluginParameters = grinder.properties.getPropertySubset("grinder.plugin.parameter.")

# Parse global parameters.
control = HTTPPluginControl.getConnectionDefaults()
control.followRedirects = pluginParameters.getBoolean("followRedirects", 0)
control.useCookies = pluginParameters.getBoolean("useCookies", 1)
logHTML = pluginParameters.getBoolean("logHTML", 0)

if pluginParameters["disablePersistentConnections"]:
    control.defaultHeaders = ( NVPair("Connection", "close"), )


class G2HTTPTest:
    """Parses parameters for an individual test and records the test
    invocation using a G3 Test."""

    def __init__(self, testNumber, properties):
        self.sleepTime = properties["sleepTime"]

        headers = []
        seenContentType = 0

        for e in properties.getPropertySubset("parameter.header.").entrySet():
            headers.append(NVPair(e.key, e.value))
            if not seenContentType and e.key.lower() == "content-type":
                seenContentType = 1

        postDataFilename = properties["parameter.post"]

        if postDataFilename:
            file = open(postDataFilename)
            self.postData = file.read()
            file.close()

            if not seenContentType:
                headers.append(NVPair("Content-type",
                                      "application/x-www-form-urlencoded"))

        else: self.postData = None

        self.okString = properties["parameter.ok"]
        self.url = properties["parameter.url"]

        realm = properties["basicAuthenticationRealm"]
        user = properties["basicAuthenticationUser"]
        password = properties["basicAuthenticationPassword"]

        if realm and user and password:
            self.basicAuthentication = (realm, user, password)

        elif not realm and not user and not password:
            self.basicAuthentication = None

        else:
            raise "If you specify one of { basicAuthenticationUser, basicAuthenticationRealm, basicAuthenticationPassword } you must specify all three."

        self.request = HTTPRequest(headers=headers)
        self.test = Test(testNumber, properties["description"])
        self.test.record(self.request)

    def doTest(self, iteration):

        if self.basicAuthentication:
            connection = HTTPPluginControl.getThreadConnection(self.url)

            connection.addBasicAuthorization(self.basicAuthentication[0],
                                             self.basicAuthentication[1],
                                             self.basicAuthentication[2])

        grinder.statistics.delayReports = 1

        if self.postData:
            page = self.request.POST(self.url, self.postData).text
        else:
            page = self.request.GET(self.url).text

        if not page:
            error = self.okString
        else:
            error = self.okString and page.find(self.okString) == -1

            if error or logHTML:
                if self.test.description:
                    description = "_%s" % self.test.description
                else:
                    description = ""

                filename = grinder.filenameFactory.createFilename(
                    "page",
                    "_%d_%.3d%s" % (iteration, self.test.number, description))

                file = open(filename, "w")
                print >> file, page
                file.close()

                if error:
                    grinder.logger.error(
                        "The 'ok' string ('%s') was not found in the page "
                        "received. The output has been written to '%s'." %
                        (self.okString, filename))

        if error:
            grinder.statistics.forLastTest.success = 0

        if self.sleepTime:
            grinder.sleep(long(self.sleepTime))

# Parse per-test parameters.
testProperties = grinder.properties.getPropertySubset("grinder.test")
tests = {}

for e in testProperties.entrySet():
    n = int(e.key.split('.')[0])

    if not tests.get(n):
        tests[n] = G2HTTPTest(n, testProperties.getPropertySubset("%s." % n))

sortedTests = tests.items()
sortedTests.sort()

# Our TestRunner simply iterates over the tests for each run.
class TestRunner:
    def __init__(self):
        self.iteration = 0

    def __call__(self):
        for testNumber,g2HTTPTest in sortedTests:
            g2HTTPTest.doTest(self.iteration)

        self.iteration += 1
