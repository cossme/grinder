# Using The Grinder with other test frameworks
#
# Example showing how The Grinder can be used with HTTPUnit.
#
# Copyright (C) 2003, 2004 Tony Lodge
# Copyright (C) 2004 Philip Aston
# Distributed under the terms of The Grinder license.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test

from com.zaplet.test.frontend.http import HttpTest

# These correspond to method names on the test class.
testNames = [ "testRedirect",
              "testRefresh",
              "testNegativeLogin",
              "testLogin",
              "testPortal",
              "testHeader",
              "testAuthoringLink",
              "testTemplateDesign",
              "testSearch",
              "testPreferences",
              "testAboutZaplet",
              "testHelp",
              "testLogoutLink",
              "testNavigationFrame",
              "testBlankFrame",
              "testContentFrame",
              "testLogout", ]

tests=[]

for name, i in zip(testNames, range(len(testNames))):
  t = HttpTest(name)
  Test(i, name).record(t)
  tests.append(t)

# A TestRunner instance is created for each thread. It can be used to
# store thread-specific data.
class TestRunner:
    def __call__(self):
        for t in tests:
            result = t.run()

