// Copyright (C) 2008 - 2013 Philip Aston
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

import static net.grinder.testutility.AssertUtilities.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.regex.Pattern;

import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.util.Directory;
import net.grinder.util.Directory.DirectoryException;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link CacheParametersImplementation}.
 *
 * @author Philip Aston
 */
public class TestCacheParametersImplementation {

  private Directory m_directory1;
  private Directory m_directory2;
  private Pattern m_pattern1;
  private Pattern m_pattern2;

  @Before
  public void setUp() throws DirectoryException {
    m_directory1 = new Directory(new File("blah"));
    m_directory2 = new Directory(new File("blurghah"));
    m_pattern1 = Pattern.compile(".*");
    m_pattern2 = Pattern.compile(".?");
  }

  @Test
  public void testBasics() throws Exception {
    final CacheParameters cacheParameters1 =
      new CacheParametersImplementation(m_directory1, m_pattern1);
    final CacheParameters cacheParameters2 =
      new CacheParametersImplementation(m_directory2, m_pattern1);
    final CacheParameters cacheParameters3 =
      new CacheParametersImplementation(m_directory2, m_pattern2);
    final CacheParameters cacheParameters4 =
      new CacheParametersImplementation(m_directory1, m_pattern1);

    assertEquals(m_directory1, cacheParameters1.getDirectory());
    assertEquals(m_pattern1.pattern(),
                 cacheParameters1.getFileFilterPattern().pattern());
    assertEquals(m_directory2, cacheParameters2.getDirectory());

    assertEquals(cacheParameters1, cacheParameters1);
    assertEquals(cacheParameters1.hashCode(), cacheParameters1.hashCode());

    assertEquals(cacheParameters1, cacheParameters4);
    assertEquals(cacheParameters1.hashCode(), cacheParameters4.hashCode());

    assertFalse(cacheParameters1.equals(cacheParameters2));
    assertFalse(cacheParameters2.equals(cacheParameters3));
    assertFalse(cacheParameters2.equals(cacheParameters1));
    assertFalse(cacheParameters2.equals(null));

    assertFalse(cacheParameters2.equals(this));

  }

  @Test
  public void testCreateCacheHighWaterMark() throws Exception {
    final CacheParameters cache1 =
      new CacheParametersImplementation(m_directory1, m_pattern1);
    final CacheParameters cache2 =
      new CacheParametersImplementation(m_directory2, m_pattern2);

    final CacheHighWaterMark a = cache1.createHighWaterMark(100);
    final CacheHighWaterMark b = cache1.createHighWaterMark(100);
    final CacheHighWaterMark c = cache2.createHighWaterMark(120);

    assertEquals(100, a.getTime());
    assertEquals(100, b.getTime());
    assertEquals(120, c.getTime());

    assertTrue(a.isForSameCache(b));
    assertTrue(a.isForSameCache(a));
    assertTrue(b.isForSameCache(a));
    assertFalse(a.isForSameCache(c));
    assertFalse(c.isForSameCache(a));
    assertFalse(c.isForSameCache(
      new CacheHighWaterMark() {
        @Override
        public long getTime() {
          return 120;
        }

        @Override
        public boolean isForSameCache(final CacheHighWaterMark other) {
          return true;
        }
      }
    ));
  }

  @Test
  public void testCacheHighWaterMarkEquality() throws Exception {
    final CacheParameters cache1 =
        new CacheParametersImplementation(m_directory1, m_pattern1);

    final CacheHighWaterMark a = cache1.createHighWaterMark(100);
    assertEquals(a, a);
    assertEquals(a.hashCode(), a.hashCode());
    assertNotEquals(a, null);
    assertNotEquals(a, this);

    final CacheHighWaterMark b = cache1.createHighWaterMark(100);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    final CacheHighWaterMark c = cache1.createHighWaterMark(101);
    assertNotEquals(a, c);

    final CacheParameters cache2 =
        new CacheParametersImplementation(m_directory1, m_pattern2);

    final CacheHighWaterMark d = cache2.createHighWaterMark(100);
    assertNotEquals(a, d);
  }
}
