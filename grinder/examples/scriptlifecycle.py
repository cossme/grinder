# The script life cycle
#
# A script that demonstrates how the various parts of a script and
# their effects on worker threads.

# The "top level" of the script is called once for each worker
# process. Perform any one-off initialisation here. For example,
# import all the modules, set up shared data structures, and declare
# all the Test objects you will use.

from net.grinder.script.Grinder import grinder
from java.lang import System

# The totalNumberOfRuns variable is shared by all worker threads.
totalNumberOfRuns = 0

# An instance of the TestRunner class is created for each worker thread.
class TestRunner:

    # There's a runsForThread variable for each worker thread. This
    # statement specifies a class-wide initial value.
    runsForThread = 0

    # The __init__ method is called once for each thread.
    def __init__(self):
        # There's an initialisationTime variable for each worker thread.
        self.initialisationTime = System.currentTimeMillis()

        grinder.logger.info("New thread started at time %s" %
                            self.initialisationTime)

    # The __call__ method is called once for each test run performed by
    # a worker thread.
    def __call__(self):

        # We really should synchronise this access to the shared
        # totalNumberOfRuns variable. See JMS receiver example for how
        # to use the Python Condition class.
        global totalNumberOfRuns
        totalNumberOfRuns += 1

        self.runsForThread += 1

        grinder.logger.info(
            "runsForThread=%d, totalNumberOfRuns=%d, initialisationTime=%d" %
            (self.runsForThread, totalNumberOfRuns, self.initialisationTime))

        # You can also vary behaviour based on thread ID.
        if grinder.threadNumber % 2 == 0:
            grinder.logger.info("I have an even thread ID.")

    # Scripts can optionally define a __del__ method. The Grinder
    # guarantees this will be called at shutdown once for each thread
    # It is useful for closing resources (e.g. database connections)
    # that were created in __init__.
    def __del__(self):
        grinder.logger.info("Thread shutting down")
