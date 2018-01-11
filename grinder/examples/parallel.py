# Run test scripts in parallel
#
# Run TestScript1 in 50% of threads, TestScript2 in 25% of threads,
# and TestScript3 in 25% of threads.

from net.grinder.script.Grinder import grinder

scripts = ["TestScript1", "TestScript2", "TestScript3"]

# Ensure modules are initialised in the process thread.
for script in scripts: exec("import %s" % script)

def createTestRunner(script):
    exec("x = %s.TestRunner()" % script)
    return x

class TestRunner:
    def __init__(self):
        tid = grinder.threadNumber

        if tid % 4 == 2:
            self.testRunner = createTestRunner(scripts[1])
        elif tid % 4 == 3:
            self.testRunner = createTestRunner(scripts[2])
        else:
            self.testRunner = createTestRunner(scripts[0])

    # This method is called for every run.
    def __call__(self):
        self.testRunner()