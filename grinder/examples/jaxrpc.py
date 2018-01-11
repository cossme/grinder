# JAX-RPC Web Service
#
# Exercise a basic Web Service from the BEA WebLogic Server 7.0
# examples.
#
# Before running this example you will need to add the generated
# JAX-RPC client classes and webserviceclient.jar to your CLASSPATH.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from examples.webservices.basic.javaclass import HelloWorld_Impl
from java.lang import System

System.setProperty( "javax.xml.rpc.ServiceFactory",
 "weblogic.webservice.core.rpc.ServiceFactoryImpl")

webService = HelloWorld_Impl("http://localhost:7001/basic_javaclass/HelloWorld?WSDL")

port  = webService.getHelloWorldPort()
Test(1, "JAXP Port test").record(port)

class TestRunner:
    def __call__(self):
        result = port.sayHello(grinder.threadNumber, grinder.grinderID)
        grinder.logger.info("Got '%s'" % result)
