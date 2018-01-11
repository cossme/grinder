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

import java.io.FileFilter;
import java.util.regex.Pattern;

import net.grinder.util.Directory;



/**
 * File Distribution. Has a model of the agent cache state, and is a factory for
 * {@link FileDistributionHandler}s.
 *
 * @author Philip Aston
 */
public interface FileDistribution extends FileChangeWatcher {

  /**
   * Accessor for our {@link AgentCacheState}.
   *
   * @return The agent cache state.
   */
  AgentCacheState getAgentCacheState();

  /**
   * Get a {@link FileDistributionHandler} for a new file
   * distribution.
   *
   * <p>The FileDistributionHandler updates our simple model of the
   * remote cache state. Callers should only use one
   * FileDistributionHandler at a time for a given FileDistribution.
   * Using multiple instances concurrently will result in undefined
   * behaviour.</p>
   *
   * @return Handler for new file distribution.
   */
  FileDistributionHandler getHandler();

  /**
   * Scan the given directory for files that have been recently modified. Update
   * the agent cache state appropriately. Notify our listeners if changed files
   * are discovered.
   */
  void scanDistributionFiles();

  /**
   * Return a FileFilter that can be used to test  whether the given file is
   * one that will be distributed.
   *
   * @return The filter.
   */
  FileFilter getDistributionFileFilter();


  /**
   * Update the distribution directory.
   *
   * @param directory The base distribution directory.
   */
  void setDirectory(Directory directory);

  /**
   * Update the pattern used to filter out files that shouldn't be distributed.
   *
   * @param distributionFileFilterPattern -
   *            The filter. Files with names that match this pattern will be
   *            filtered out.
   */
  void setFileFilterPattern(Pattern distributionFileFilterPattern);
}
