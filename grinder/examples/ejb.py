# Enterprise Java Beans
#
# Exercise a stateful session EJB from the Oracle WebLogic Server
# examples. Additionally this script demonstrates the use of the
# ScriptContext sleep(), getThreadId() and getRunNumber() methods.
#
# Before running this example you will need to add the EJB client and
# the WebLogic classes to your CLASSPATH.

from java.lang import String
from java.util import Properties,Random
from javax.naming import Context,InitialContext
from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from weblogic.jndi import WLInitialContextFactory

tests = {
    "home" : Test(1, "TraderHome"),
    "trade" : Test(2, "Trader buy/sell"),
    "query" : Test(3, "Trader getBalance"),
    }

# Initial context lookup for EJB home.
p = Properties()
p[Context.INITIAL_CONTEXT_FACTORY] = WLInitialContextFactory.name

home = InitialContext(p).lookup("ejb20-statefulSession-TraderHome")
tests["home"].record(home)

random = Random()

class TestRunner:
    def __call__(self):
        log = grinder.logger.info

        trader = home.create()
        tests["trade"].record(trader.sell)
        tests["trade"].record(trader.buy)
        tests["query"].record(trader.getBalance)

        stocksToSell = { "BEAS" : 100, "MSFT" : 999 }
        for stock, amount in stocksToSell.items():
            tradeResult = trader.sell("John", stock, amount)
            log("Result of trader.sell(): %s" % tradeResult)

        grinder.sleep(100)              # Idle a while

        stocksToBuy = { "BEAS" : abs(random.nextInt()) % 1000 }
        for stock, amount in stocksToBuy.items():
            tradeResult = trader.buy("Phil", stock, amount)
            log("Result of trader.buy(): %s" % tradeResult)

        balance = trader.getBalance()
        log("Balance is $%.2f" % balance)

        trader.remove()                 # We don't record the remove() as a test


