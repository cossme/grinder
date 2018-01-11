# Test script which generates some random data for testing the
# console.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from java.util import Random
from java.lang import Math

r = Random()

class Tester:
    def __init__(self, i):
        self.i = i

    def __call__(self):
        t = 500 + r.nextGaussian() * self.i * 10
        grinder.sleep(int(t), 0)

def createTester(i):
    result = Tester(i)
    Test(i, "Test %s" % i).record(result)
    return result

testers = [ createTester(i) for i in range(0, 10) ]

class TestRunner:
    def __call__(self):
        statistics = grinder.statistics

#        statistics.delayReports = 1

        for tester in testers:
            tester()


