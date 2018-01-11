// Copyright (C) 2005 - 2008 Philip Aston
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

import java.io.File;

import net.grinder.communication.Address;
import net.grinder.console.communication.DistributionControl;
import net.grinder.console.distribution.AgentSet.OutOfDateException;
import net.grinder.util.FileContents;


/**
 * File Distribution Handler implementation.
 *
 * <p>Not thread safe.</p>
 *
 * @author Philip Aston
 */
final class FileDistributionHandlerImplementation
  implements FileDistributionHandler {

  private final CacheParameters m_cacheParameters;
  private final File m_directory;
  private final File[] m_files;
  private final long m_latestFileTime;
  private final DistributionControl m_distributionControl;
  private final AgentSet m_agents;

  private int m_fileIndex = 0;

  FileDistributionHandlerImplementation(
    CacheParameters cacheParameters,
    File directory,
    File[] files,
    DistributionControl distributionControl,
    AgentSet agents) {

    m_cacheParameters = cacheParameters;
    m_directory = directory;
    m_files = files;
    m_distributionControl = distributionControl;
    m_agents = agents;

    long latestFileTime = -1;

    for (int i = 0; i < m_files.length; ++i) {
      final long fileTime =
        new File(m_directory, m_files[i].getPath()).lastModified();

      latestFileTime = Math.max(latestFileTime, fileTime);
    }

    m_latestFileTime = latestFileTime;
  }

  public Result sendNextFile() throws FileContents.FileContentsException {
    try {
      if (m_fileIndex < m_files.length) {
        if (m_fileIndex == 0) {
          // Clear any cache that has out of date cache parameters.
          // We currently we do nothing about cached copies of deleted files.
          final Address addressAgentsWithInvalidCaches =
            m_agents.getAddressOfOutOfDateAgents(0);

          m_distributionControl.clearFileCaches(addressAgentsWithInvalidCaches);
        }

        try {
          final int index = m_fileIndex;
          final File file = m_files[index];

          final Address addressAgentsWithoutFile =
            m_agents.getAddressOfOutOfDateAgents(
              new File(m_directory, file.getPath()).lastModified());

          m_distributionControl.sendFile(addressAgentsWithoutFile,
                                         new FileContents(m_directory, file));

          return new Result() {
              public int getProgressInCents() {
                return ((index + 1) * 100) / m_files.length;
              }

              public String getFileName() {
                return file.getPath();
              }
            };
        }
        finally {
          ++m_fileIndex;
        }
      }
      else {
        m_distributionControl.setHighWaterMark(
          m_agents.getAddressOfAllAgents(),
          m_cacheParameters.createHighWaterMark(m_latestFileTime));

        return null;
      }
    }
    catch (OutOfDateException e) {
      return null;
    }
  }
}
