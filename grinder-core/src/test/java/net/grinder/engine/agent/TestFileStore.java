// Copyright (C) 2004 - 2013 Philip Aston
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

package net.grinder.engine.agent;

import static net.grinder.testutility.AssertUtilities.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.SimpleMessage;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.agent.ClearCacheMessage;
import net.grinder.messages.agent.DistributeFileMessage;
import net.grinder.messages.agent.DistributionCacheCheckpointMessage;
import net.grinder.messages.agent.StubCacheHighWaterMark;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.FileUtilities;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;

import org.junit.Test;
import org.slf4j.Logger;


/**
 *  Unit tests for {@code FileStore}.
 *
 * @author Philip Aston
 */
public class TestFileStore extends AbstractJUnit4FileTestCase {

  private static final Random s_random = new Random();

  @Test public void testConstruction() throws Exception {

    File.createTempFile("file", "", getDirectory());
    assertEquals(1, getDirectory().list().length);

    final FileStore fileStore = new FileStore(getDirectory(), null);
    final File currentDirectory = fileStore.getDirectory().getFile();
    assertNotNull(currentDirectory);

    assertTrue(
      currentDirectory.getPath().startsWith(getDirectory().getPath()));

    // No messages have been received, so no physical directories will
    // have been created yet.

    assertEquals(1, getDirectory().list().length);
    assertTrue(!currentDirectory.exists());

    // Can't use a plain file.
    final File file1 = File.createTempFile("file", "", getDirectory());

    try {
      new FileStore(file1, null);
      fail("Expected FileStoreException");
    }
    catch (final FileStore.FileStoreException e) {
    }

    // Nor a directory that contains a plain file clashing with one
    // of the subdirectory names.
    assertTrue(file1.delete());
    assertTrue(file1.mkdir());
    assertTrue(new File(file1, "current").createNewFile());

    try {
      new FileStore(file1, null);
      fail("Expected FileStoreException");
    }
    catch (final FileStore.FileStoreException e) {
    }

    // Can't use a read-only directory.
    final File readOnlyDirectory = new File(getDirectory(), "directory");
    assertTrue(readOnlyDirectory.mkdir());
    assertTrue(readOnlyDirectory.setReadOnly());

    try {
      new FileStore(readOnlyDirectory, null);
      fail("Expected FileStoreException");
    }
    catch (final FileStore.FileStoreException e) {
    }

    // Perfectly fine to create a FileStore around a directory that
    // doens't yet exist.
    final File notThere = new File(getDirectory(), "notThere");
    new FileStore(notThere, null);
  }

  @Test public void testSender() throws Exception {

    final Logger logger = mock(Logger.class);

    final FileStore fileStore = new FileStore(getDirectory(), logger);

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    fileStore.registerMessageHandlers(messageDispatcher);

    // Other Messages get ignored.
    final Message message0 = new SimpleMessage();
    messageDispatcher.send(message0);
    verifyNoMoreInteractions(logger);

    // Shutdown does nothing.
    messageDispatcher.shutdown();
    verifyNoMoreInteractions(logger);

    // Test with a good message.
    final File sourceDirectory = new File(getDirectory(), "source");
    assertTrue(sourceDirectory.mkdirs());

    final File file0 = new File(sourceDirectory, "dir/file0");
    assertTrue(file0.getParentFile().mkdirs());
    final OutputStream outputStream = new FileOutputStream(file0);
    final byte[] bytes = new byte[500];
    s_random.nextBytes(bytes);
    outputStream.write(bytes);
    outputStream.close();

    final FileContents fileContents0 =
      new FileContents(sourceDirectory, new File("dir/file0"));

    final File readmeFile = new File(getDirectory(), "README.txt");
    final File incomingDirectoryFile = new File(getDirectory(), "incoming");
    final File currentDirectoryFile = new File(getDirectory(), "current");
    assertEquals(currentDirectoryFile, fileStore.getDirectory().getFile());

    // Before message sent, none of our files or directories exist.
    assertTrue(!readmeFile.exists());
    assertTrue(!incomingDirectoryFile.exists());
    assertTrue(!currentDirectoryFile.exists());

    final Message message1 = new DistributeFileMessage(fileContents0);

    // Can't receive a DFM if the incoming directory can't be created.
    FileUtilities.setCanAccess(getDirectory(), false);

    try {
      messageDispatcher.send(message1);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }

    FileUtilities.setCanAccess(getDirectory(), true);

    verify(logger).error(contains("Could not create directory"));

    assertFalse(incomingDirectoryFile.delete());

    messageDispatcher.send(message1);
    verify(logger).info(contains("Updating file store"),
                        isA(FileContents.class));

    // Message has been sent, the incoming directory and the read me exist.
    assertTrue(readmeFile.exists());
    assertTrue(incomingDirectoryFile.exists());
    assertTrue(!currentDirectoryFile.exists());

    final File targetFile = new File(incomingDirectoryFile, "dir/file0");
    assertTrue(targetFile.canRead());

    assertEquals(currentDirectoryFile, fileStore.getDirectory().getFile());

    // Now getDirectory() has been called, both directories exist.
    assertTrue(readmeFile.exists());
    assertTrue(incomingDirectoryFile.exists());
    assertTrue(currentDirectoryFile.exists());

    // Frig with currentDirectory so that getDirectory() fails.
    new Directory(currentDirectoryFile).deleteContents();
    assertTrue(currentDirectoryFile.delete());
    assertTrue(currentDirectoryFile.createNewFile());

    try {
      fileStore.getDirectory();
      fail("Expected FileStoreException");
    }
    catch (final FileStore.FileStoreException e) {
    }

    // Put things back again.
    assertTrue(currentDirectoryFile.delete());
    fileStore.getDirectory();

    // Test with a bad message.
    assertTrue(targetFile.setReadOnly());

    try {
      messageDispatcher.send(message1);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }

    verify(logger, times(2)).info(contains("Updating file store"),
                                  isA(FileContents.class));

    verify(logger).error(contains("Failed to create file"));

    final Message message2 = new ClearCacheMessage();

    FileUtilities.setCanAccess(targetFile, false);
    // UNIX: Permission to remove a file is set on directory.
    FileUtilities.setCanAccess(targetFile.getParentFile(), false);

    try {
      messageDispatcher.send(message2);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }

    FileUtilities.setCanAccess(targetFile.getParentFile(), true);
    FileUtilities.setCanAccess(targetFile, true);

    verify(logger).info(contains("Clearing file store"));
    verify(logger).error(contains("Could not delete"));

    messageDispatcher.send(message2);
    verify(logger, times(2)).info(contains("Clearing file store"));
    verifyNoMoreInteractions(logger);

    assertTrue(!targetFile.canRead());

    assertEquals(currentDirectoryFile, fileStore.getDirectory().getFile());
  }

  @Test public void testFileStoreException() throws Exception {
    final Exception nested = new Exception("");
    final FileStore.FileStoreException e =
      new FileStore.FileStoreException("bite me", nested);

    assertEquals(nested, e.getCause());
  }

  @Test public void testDistributionCheckpointMessage() throws Exception {
    final FileStore fileStore = new FileStore(getDirectory(), null);

    final CacheHighWaterMark outOfDateCacheHighWaterMark =
      fileStore.getCacheHighWaterMark();
    assertEquals(-1, outOfDateCacheHighWaterMark.getTime());
    assertFalse(outOfDateCacheHighWaterMark.isForSameCache(
                  outOfDateCacheHighWaterMark));

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    fileStore.registerMessageHandlers(messageDispatcher);

    final CacheHighWaterMark cacheHighWaterMark =
      new StubCacheHighWaterMark("", 123);
    final Message message =
      new DistributionCacheCheckpointMessage(cacheHighWaterMark);

    messageDispatcher.send(message);

    assertEquals(cacheHighWaterMark, fileStore.getCacheHighWaterMark());
  }

  @Test
  public void testOutOfDataCachetHighWaterMark() throws Exception {
    final CacheHighWaterMark hwm =
        new FileStore(getDirectory(), null).getCacheHighWaterMark();
    assertEquals(hwm, hwm);
    assertEquals(hwm.hashCode(), hwm.hashCode());
    assertNotEquals(hwm, this);
    assertNotEquals(hwm, null);

    final CacheHighWaterMark hwm2 =
        new FileStore(getDirectory(), null).getCacheHighWaterMark();
    assertEquals(hwm, hwm2);
    assertEquals(hwm.hashCode(), hwm2.hashCode());
    assertEquals(hwm.toString(), hwm2.toString());
  }
}
