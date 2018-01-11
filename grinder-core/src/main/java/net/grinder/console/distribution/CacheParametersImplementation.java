// Copyright (C) 2008 - 2012 Philip Aston
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

import java.io.Serializable;
import java.util.regex.Pattern;

import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.util.Directory;


/**
 * Implementation of {@link CacheParameters}.
 *
 * @author Philip Aston
 */
final class CacheParametersImplementation
  implements CacheParameters, Serializable {

  private static final long serialVersionUID = 1L;

  private final Directory m_directory;
  private final Pattern m_fileFilterPattern;

  public CacheParametersImplementation(Directory directory,
                                       Pattern fileFilterPattern) {
    m_directory = directory;
    m_fileFilterPattern = fileFilterPattern;
  }

  public Directory getDirectory() {
    return m_directory;
  }

  public Pattern getFileFilterPattern() {
    return m_fileFilterPattern;
  }

  public CacheHighWaterMark createHighWaterMark(long time) {
    return new CacheHighWaterMarkImplementation(this, time);
  }

  @Override public int hashCode() {
    return m_directory.hashCode() ^ m_fileFilterPattern.pattern().hashCode();
  }

  @Override public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final CacheParametersImplementation other =
          (CacheParametersImplementation)o;

    return m_directory.equals(other.m_directory) &&
           m_fileFilterPattern.pattern().equals(
             other.m_fileFilterPattern.pattern());
  }

  private static final class CacheHighWaterMarkImplementation
    implements CacheHighWaterMark {

    private static final long serialVersionUID = 1L;

    private final CacheParameters m_cacheParameters;
    private final long m_time;

    public CacheHighWaterMarkImplementation(CacheParameters cacheParameters,
                                            long time) {
      m_cacheParameters = cacheParameters;
      m_time = time;
    }

    public boolean isForSameCache(CacheHighWaterMark other) {
      if (!(other instanceof CacheHighWaterMarkImplementation)) {
        return false;
      }

      final CacheHighWaterMarkImplementation otherHighWater =
        (CacheHighWaterMarkImplementation)other;

      return m_cacheParameters.equals(otherHighWater.m_cacheParameters);
    }

    public long getTime() {
      return m_time;
    }
  }
}
