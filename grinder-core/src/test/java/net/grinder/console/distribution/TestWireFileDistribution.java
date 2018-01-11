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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.TimerTask;
import java.util.regex.Pattern;

import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.StubTimer;
import net.grinder.translation.Translations;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link WireFileDistribution}.
 *
 * @author Philip Aston
 */
public class TestWireFileDistribution extends AbstractJUnit4FileTestCase {

  @Mock private MessageDispatchRegistry m_messageDispatchRegistry;
  @Mock private ConsoleCommunication m_consoleCommunication;
  @Mock private Translations m_translations;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);
  }

  @Test public void testWireFileDistribution() throws Exception {

    final FileDistribution fileDistribution = mock(FileDistribution.class);

    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_translations, new File(getDirectory(), "props"));

    final StubTimer timer = new StubTimer();

    new WireFileDistribution(fileDistribution,
                             consoleProperties,
                             timer);

    assertEquals(6000, timer.getLastDelay());
    assertEquals(6000, timer.getLastPeriod());

    final TimerTask scanFileTask = timer.getLastScheduledTimerTask();
    scanFileTask.run();
    verify(fileDistribution).scanDistributionFiles();

    consoleProperties.setDistributionFileFilterExpression(".*");

    final ArgumentCaptor<Pattern> patternCaptor =
      ArgumentCaptor.forClass(Pattern.class);

    verify(fileDistribution).setFileFilterPattern(patternCaptor.capture());
    assertEquals(".*", patternCaptor.getValue().pattern());

    final ArgumentCaptor<Directory> directoryCaptor =
      ArgumentCaptor.forClass(Directory.class);

    final Directory directory = new Directory(new File(getDirectory(), "foo"));
    consoleProperties.setAndSaveDistributionDirectory(directory);

    verify(fileDistribution).setDirectory(directoryCaptor.capture());
    assertSame(directory, directoryCaptor.getValue());

    consoleProperties.setConsolePort(999);

    verifyNoMoreInteractions(fileDistribution);
  }
}
