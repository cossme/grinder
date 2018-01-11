// Copyright (C) 2008 - 2011 Philip Aston
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

package net.grinder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import net.grinder.common.GrinderException;
import net.grinder.testutility.AssertUtilities;
import net.grinder.util.AbstractMainClass.LoggedInitialisationException;
import net.grinder.util.JVM.VersionException;

import org.junit.Test;
import org.slf4j.Logger;


/**
 * Unit tests for {@link AbstractMainClass}.
 *
 * @author Philip Aston
 */
public class TestAbstractMainClass {

  @Test public void testAbstractMainClass() throws Exception {

    final Logger logger = mock(Logger.class);
    final String myUsage = "do some stuff";

    final MyMainClass mainClass = new MyMainClass(logger, myUsage);

    assertSame(logger, mainClass.getLogger());

    final String javaVersion = System.getProperty("java.version");

    try {
      try {
        System.setProperty("java.version", "whatever");
        new MyMainClass(logger, myUsage);
        fail("Expected VersionException");
      }
      catch (VersionException e) {
      }

      try {
        System.setProperty("java.version", "1.3");
        new MyMainClass(logger, myUsage);
        fail("Expected LoggedInitialisationException");
      }
      catch (LoggedInitialisationException e) {
        AssertUtilities.assertContains(e.getMessage(), "Unsupported");
        verify(logger).error(contains("incompatible version"),
                             isA(JVM.class),
                             isA(String.class));
      }
    }
    finally {
      System.setProperty("java.version", javaVersion);
    }

    final LoggedInitialisationException barfError = mainClass.barfError("foo");
    assertEquals("foo", barfError.getMessage());
    verify(logger).error(contains("foo"));

    final LoggedInitialisationException barfUsage = mainClass.barfUsage();
    AssertUtilities.assertContains(barfUsage.getMessage(), myUsage);

    verify(logger).error(contains(myUsage));
    verifyNoMoreInteractions(logger);

  }

  private static class MyMainClass extends AbstractMainClass {
    public MyMainClass(Logger logger, String usage) throws GrinderException {
      super(logger, usage);
    }
  }
}
