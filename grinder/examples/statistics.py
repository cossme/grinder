# Accessing test statistics
#
# Examples of using The Grinder statistics API with standard
# statistics.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest


class TestRunner:
    def __call__(self):
        request = HTTPRequest(url = "http://localhost:7001")
        Test(1, "Basic request").record(request)

        # Example 1. You can get the time of the last test as follows.
        result = request.GET("index.html")

        grinder.logger.info("The last test took %d milliseconds" %
                            grinder.statistics.forLastTest.time)


        # Example 2. Normally test results are reported automatically
        # when the test returns. If you want to alter the statistics
        # after a test has completed, you must set delayReports = 1 to
        # delay the reporting before performing the test. This only
        # affects the current worker thread.
        grinder.statistics.delayReports = 1

        result = request.GET("index.html")

        if grinder.statistics.forLastTest.time > 5:
            # We set success = 0 to mark the test as a failure. The test
            # time will be reported to the data log, but not included
            # in the aggregate statistics sent to the console or the
            # summary table.
            grinder.statistics.forLastTest.success = 0

        # With delayReports = 1 you can call report() to explicitly.
        grinder.statistics.report()

        # You can also turn the automatic reporting back on.
        grinder.statistics.delayReports = 0


        # Example 3.
        # getForCurrentTest() accesses statistics for the current test.
        # getForLastTest() accesses statistics for the last completed test.

        def page(self):
            resourceRequest =HTTPRequest(url = "http://localhost:7001")
            Test(2, "Request resource").record(resourceRequest)

            resourceRequest.GET("index.html");
            resourceRequest.GET("foo.css");

            grinder.logger.info("GET foo.css returned a %d byte body" %
                                grinder.statistics.forLastTest.getLong(
                                      "httpplugin.responseLength"))

            grinder.logger.info("Page has taken %d ms so far" %
                                grinder.statistics.forCurrentTest.time)

            if grinder.statistics.forLastTest.time > 10:
                grinder.statistics.forCurrentTest.success = 0

            resourceRequest.GET("image.gif");

        instrumentedPage = page
        Test(3, "Page").record(instrumentedPage)

        instrumentedPage(self)

