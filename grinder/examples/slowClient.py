
from net.grinder.script import Test
from net.grinder.script.Grinder import grinder
from net.grinder.plugin.http import HTTPPluginControl, HTTPRequest
from HTTPClient import NVPair

requests = {}

rates = (-1, 10000000, 1000000, 100000, 56000, 9600, 2400)

i = 0

for baud in rates:
    requests[baud] = HTTPRequest
    Test(i, "%d baud" % baud).record(requests[baud])
    i = i + 1
    

url = "http://slashdot.org/"

grinder.statistics.registerDataLogExpression("BPS", "(* 8000 (/ httpplugin.responseLength (+ (sum timedTests) (* -1 httpplugin.firstByteTime))))")
grinder.statistics.registerSummaryExpression("BPS", "(* 8000 (/ httpplugin.responseLength (+ (sum timedTests) (* -1 httpplugin.firstByteTime))))")

class TestRunner:
    def __call__(self):

        c = HTTPPluginControl.getThreadConnection(url)
        
        for baud in rates:
            c.setBandwidthLimit(baud)
            requests[baud].GET(url)

