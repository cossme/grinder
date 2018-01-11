// Copyright (C) 2005 - 2011 Philip Aston
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

package net.grinder.console.distribution;

import static net.grinder.testutility.FileUtilities.createRandomFile;

import java.io.File;

import net.grinder.communication.Address;
import net.grinder.console.communication.DistributionControl;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.FileContents;

/**
 * Unit test for {@link FileDistributionHandlerImplementation}.
 *
 * @author Philip Aston
 */
public class TestFileDistributionHandlerImplementation
  extends AbstractFileTestCase {

  private final RandomStubFactory<DistributionControl>
    m_distributionControlStubFactory =
      RandomStubFactory.create(DistributionControl.class);
  private final DistributionControl m_distributionControl =
    m_distributionControlStubFactory.getStub();

  private final RandomStubFactory<AgentSet> m_agentSetStubFactory =
    RandomStubFactory.create(AgentSet.class);
  private final AgentSet m_agentSet = m_agentSetStubFactory.getStub();

  private final CacheParameters m_cacheParameters =
    new CacheParametersImplementation(null, null);

  final File[] m_files = {
    new File("a"),
    new File("b"),
  };

  protected void setUp() throws Exception {
    super.setUp();

    for (int i = 0; i < m_files.length; ++i) {
      createRandomFile(new File(getDirectory(), m_files[i].getPath()));
    }
  }

  public void testFileDistributionHandlerImplementation() throws Exception {
    final FileDistributionHandlerImplementation fileDistributionHandler =
      new FileDistributionHandlerImplementation(
        m_cacheParameters,
        getDirectory(),
        m_files,
        m_distributionControl,
        m_agentSet);

    m_distributionControlStubFactory.assertNoMoreCalls();

    final FileDistributionHandler.Result result0 =
      fileDistributionHandler.sendNextFile();

    assertEquals(50, result0.getProgressInCents());
    assertEquals("a", result0.getFileName());

    m_distributionControlStubFactory.assertSuccess(
      "clearFileCaches", Address.class);

    m_agentSetStubFactory.assertSuccess(
      "getAddressOfOutOfDateAgents", new Long(0));

    m_distributionControlStubFactory.assertSuccess("sendFile",
                                                 Address.class,
                                                 FileContents.class);

    m_agentSetStubFactory.assertSuccess(
      "getAddressOfOutOfDateAgents",
      new Long(new File(getDirectory(), m_files[0].getPath()).lastModified()));

    m_agentSetStubFactory.assertNoMoreCalls();

    final FileDistributionHandler.Result result1 =
      fileDistributionHandler.sendNextFile();

    assertEquals(100, result1.getProgressInCents());
    assertEquals("b", result1.getFileName());

    m_distributionControlStubFactory.assertSuccess("sendFile",
                                                 Address.class,
                                                 FileContents.class);

    m_agentSetStubFactory.assertSuccess(
      "getAddressOfOutOfDateAgents",
      new Long(new File(getDirectory(), m_files[1].getPath()).lastModified()));

    m_agentSetStubFactory.assertNoMoreCalls();

    final FileDistributionHandler.Result result2 =
      fileDistributionHandler.sendNextFile();

    assertNull(result2);

    m_distributionControlStubFactory.assertSuccess(
      "setHighWaterMark", Address.class, CacheHighWaterMark.class);
    m_distributionControlStubFactory.assertNoMoreCalls();

    m_agentSetStubFactory.assertSuccess("getAddressOfAllAgents");

    m_agentSetStubFactory.assertNoMoreCalls();
  }

  public void testOutOfDateHandler() throws Exception {

    final FileDistributionHandlerImplementation fileDistributionHandler =
      new FileDistributionHandlerImplementation(
        m_cacheParameters,
        getDirectory(),
        m_files,
        m_distributionControl,
        m_agentSet);

    assertNotNull(fileDistributionHandler.sendNextFile());

    m_agentSetStubFactory.setThrows("getAddressOfOutOfDateAgents",
                                    new AgentSet.OutOfDateException());

    assertNull(fileDistributionHandler.sendNextFile());
  }
}
