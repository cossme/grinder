from net.grinder.script.Grinder import grinder

# You need to install Jython to access the threading package. See
# http://grinder.sourceforge.net/faq.html#jython-libraries
from threading import Condition


# Global lock
c = Condition()

waiting = 0
checkpointReachedForRun = -1
numberOfThreads = int(grinder.properties["grinder.threads"])


class TestRunner:
    
    # This method is called for every run.
    def __call__(self):
        # Declare the global variables that we update.
        global checkpointReachedForRun, waiting

        # Locking ensures only a single thread can be active (not
        # waiting) in the section between the acquire() and the
        # release().
        c.acquire()
        waiting += 1

        if waiting == numberOfThreads:
            # We're the last thread, wake everyone up.
            checkpointReachedForRun = grinder.runNumber
            waiting = 0            
            c.notifyAll()
        else:
            while grinder.runNumber > checkpointReachedForRun: c.wait()

        c.release()
        
        grinder.logger.output("Hello World")

        # Sleep for a random amount of time around 10 seconds.
        grinder.sleep(10000)
