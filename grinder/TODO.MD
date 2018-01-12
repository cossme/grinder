# Grinder TODO List

This is The Grinder TODO list. Its a collection of my thoughts on the
future of The Grinder.

If you want to implement something off this wish list, let me know
first by discussion your ideas on the grinder-development list. There
are instructions on how to contribute at
http://grinder.sourceforge.net/development/contributing.html.

The TODO list is part of the binary distribution. I'll accept patches
against it too :-).

- Philip Aston


## Enhancements

## Console Services

### Functional

#### Event channel?

#### Process control
Start/Stop worker by ID




## Script Distribution

Task: Distribution file filter should be dynamically settable.

## Console



### Refactoring

Consider moving overwrite / save before close / ... handling to the
model. Needs some kind of command pattern to represent choices.



#### jEdit

Replace jEdit-syntax with new jEdit syntax package when available, if
its license terms are acceptable. Apparently now available.

    Clause 2b:

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any
    part thereof, to be licensed as a whole at no charge to all third
    parties under the terms of this License.


    10. If you wish to incorporate parts of the Program into other
    free programs whose distribution conditions are different, write
    to the author to ask for permission. For software which is
    copyrighted by the Free Software Foundation, write to the Free
    Software Foundation; we sometimes make exceptions for this. Our
    decision will be guided by the two goals of preserving the free
    status of all derivatives of our free software and of promoting
    the sharing and reuse of software generally.

## Engine

Support for some sort of "Surrogate task" that is run in a pool of
threads and has its own statistics context would be good.


### Instrumentation API

Could we support a procedural "startTest()", "stopTest()" API?

Perhaps. This procedural style would certainly be more obvious
to the average user than "wrapping". OTOH, wrapping is powerful
and I'm not sure we want to support two mechanisms to instrument
code.

## Communication

Resolve anomaly that Senders throw exceptions but Receivers return null.

## Statistics

Should TPS averages be harmonic means?
http://en.wikipedia.org/wiki/Harmonic_mean

Split out serializeable kernel of TestStatisticsMap? Look hard at where the
message is deserialised.

Simplify the index classes. Move the index implementations to nested
classes of StatisticsSetImplementation and expose a single, opaque
StatsiticsSet.Index interface.


### Separate out console statistics views from summary statistics views

Needed for custom statistics that use 'period' so are valid in the
console but not in the process logs.

### max(), min()

(Requested by Venelin Mitov).

Should store max, min values against SampleStatistics, and add max(),
min() to expressions.


## TCPProxy

Tim McNerney writes:
 > Obviously, I could edit the scripts by hand. But I'd like to have
 > TCPProxy do this for me. So is there some existing method for doing
 > such filtering? Say on target suffix ("filter=.gif,.js,.css")

Change TCPProxy filters to a stream oriented model. This should cure
another one of the TCPSniffer/HTTPPlugin bug when recording large
outputs with posts? Also, consider having a filter instance pair per
connection.

Support different client certificates for proxied connection.

Allow the filters to optionally parse unknown command line options.
They would also then have to be able to contribute to the help text.
This would allow the http plugin to add options to specify a
different filename, and also to specify stdout.

Failed connection events.

## HTTP Plugin

Script support for HTTP "system property" options.

Pablo Estades Fernýndez says:
 > I need to use client cert on TCPProxy to be able to
 > record the test case but also I need to configure
 > grinder workers to run the test case presenting a
 > client cert.

** Scripting

Consider removing need to name TestRunner - instead use the result of
the script.

Add per-run statistics. This would also allow number of aborted runs
to be recorded.

Script access to global statistics.


*** Error reporting

 > It would be possible for certain classes of error (AttributeError
 > being a good example) to spew out just
 >
 >       Unknown attribute GETx at "http.py", line 9 in __call__.
 >
 > Is this what you're after?

To do this, might have to behave differently with 1 thread vs many.

## Reports

Perhaps JasperReports?

## Code

Review use of Executor. Probably want to share them.

Remove ThreadLocal from RegisteredPlugin.


## Other HTTP/HTML libraries

DeSouza, Edwin writes:
 > Instead of using:
 > http://www.innovation.ch/java/HTTPClient/index.html
 > <http://www.innovation.ch/java/HTTPClient/FAQ.html>
 >
 > How about using Jakarta Commons HttpClient (more popular and Apache
 > License):
 > http://jakarta.apache.org/commons/httpclient/index.html


Justin Spears writes:
 > I am new to grinder, however I found the built in HTTPClient a
 > little lacking in functionality.  I might suggest using
 > httpunit (http://httpunit.sourceforge.net) instead.  It
 > works well with jython, and has extremely powerful methods
 > for handling links, posts, gets, etc.
 >
 > It uses nekohtml to parse malformed HTML documents into
 > valid XML then exposes a useful DOM based on these results.
 >
 > ...

OK, this amounts to a campaign against HTTPClient!


Reasons for HTTPClient:
  - Its solid, (and not 'alpha' which is the case for
    HttpClient).
  - Its small and comprehensible.
  - It is efficient.
  - Its extremely well written.
  - Its the incumbent.

Reasons for Commons HttpClient:
  - Its actively maintained.
  - It is more modular.
  - It is richer.

Reasons for HttpUnit:
  - nekohtml, parsing support

I prefer HttpClient, HTTPClient over HttpUnit for The Grinder as they
are "closer to the wire".

*** Summary
On balance, yes HttpClient looks good and we should use it if it
proves to be efficient. I'll add it to the TODO, but its a significant
change => low priority.

Update (Oct 05): Doubts about HttpClient's scalability:
http://sourceforge.net/mailarchive/forum.php?thread_id=8372852&forum_id=2649

We perhaps need to look more closely at parsing support for
functional assertions, but I don't want to lose The Grinder's
efficiency here.

Also http://www.nogoop.com/product_16.html#compare

* Bugs

(In addition to those on Sourceforge).



# Documentation


## Forrest TODO

Create back links from javadoc. This isn't trivial, e.g. this:

      <bottom>
  <![CDATA[
  <a class="noborder" href="../whats-new.html"
  target="_top"><img src="../../images/grinder3.jpg"></img></a>
  ]]>
      </bottom>

only works for top level javadoc.


How to include arbitrary HTML (e.g. for poll forms?)
