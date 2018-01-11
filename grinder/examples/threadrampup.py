# Thread ramp up
#
# A simple way to start threads at different times.
#

from net.grinder.script.Grinder import grinder

def log(message):
    grinder.logger.info(message)

class TestRunner:
    def __init__(self):
        log("initialising")

    def initialSleep( self):
        sleepTime = grinder.threadNumber * 5000  # 5 seconds per thread
        grinder.sleep(sleepTime, 0)
        log("initial sleep complete, slept for around %d ms" % sleepTime)

    def __call__( self ):
        if grinder.runNumber == 0: self.initialSleep()

        grinder.sleep(500)
        log("in __call__()")
