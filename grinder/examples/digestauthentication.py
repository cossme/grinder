# HTTP digest authentication
#
# Basically delegates to HTTPClient's support for digest
# authentication.
#
# Copyright (C) 2008 Matt Moran
# Copyright (C) 2008 Philip Aston
# Distributed under the terms of The Grinder license.

from net.grinder.plugin.http import HTTPPluginControl
from HTTPClient import AuthorizationInfo

# Enable HTTPClient's authorisation module.
HTTPPluginControl.getConnectionDefaults().useAuthorizationModule = 1

test1 = Test(1, "Request resource")
request1 = HTTPRequest()
test1.record(request1)

class TestRunner:
    def __call__(self):
        threadContextObject = HTTPPluginControl.getThreadHTTPClientContext()

        # Set the authorisation details for this worker thread.
        AuthorizationInfo.addDigestAuthorization(
            "www.my.com", 80, "myrealm", "myuserid", "mypw", threadContextObject)

        result = request1.GET('http://www.my.com/resource')
