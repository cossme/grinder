# Hello World, with functions
#
# The Hello World example re-written using functions.
#
# In previous examples we've defined TestRunner as a class; calling
# the class creates an instance and calling that instance invokes its
# __call__ method. This script is for the Luddites amongst you and
# shows how The Grinder engine is quite happy as long as the script
# creates a callable thing called TestRunner that can be called to
# create another callable thing.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test

test1 = Test(1, "Log method")
test1.record(grinder.logger.info)

def doRun():
    grinder.logger.info("Hello World")

def TestRunner():
    return doRun
