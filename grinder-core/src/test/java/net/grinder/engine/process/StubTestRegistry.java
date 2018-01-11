// Copyright (C) 2004 - 2011 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import static org.mockito.Mockito.mock;
import net.grinder.script.TestRegistry;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;

/**
 * Test utility that allows TestRegistryImplementation to be set from outside
 * package.
 *
 * @author Philip Aston
 */
public class StubTestRegistry {

  public static TestRegistry stubTestRegistry(Instrumenter instrumenter) {

    final TestStatisticsHelper testStatisticsHelper =
      mock(TestStatisticsHelper.class);

    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestRegistryImplementation testRegistry =
      new TestRegistryImplementation(null,
                                     statisticsSetFactory,
                                     testStatisticsHelper,
                                     null);

    testRegistry.setInstrumenter(instrumenter);

    return testRegistry;
  }
}
