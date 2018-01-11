// Copyright (C) 2005 - 2009 Philip Aston
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
import java.io.FileFilter;
import java.util.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.distribution.FileChangeWatcher.FileChangedListener;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Directory;


/**
 * Unit test for {@link FileDistributionImplementation}.
 *
 * @author Philip Aston
 */
public class TestFileDistribution extends AbstractFileTestCase {

  private final Pattern m_matchIgnoredPattern =
    Pattern.compile("^.grinder/$");
  private final Pattern m_matchAllPattern = Pattern.compile(".*");

  private final RandomStubFactory<ProcessControl> m_processControlStubFactory =
    RandomStubFactory.create(ProcessControl.class);
  private final ProcessControl m_processControl =
    m_processControlStubFactory.getStub();

  public void testGetHandler() throws Exception {
    final RandomStubFactory<DistributionControl> distributionControlStubFactory =
      RandomStubFactory.create(DistributionControl.class);
    final DistributionControl distributionControl =
      distributionControlStubFactory.getStub();

    final Directory directory1 = new Directory(getDirectory());

    final FileDistributionImplementation fileDistribution =
      new FileDistributionImplementation(distributionControl,
                                         m_processControl,
                                         directory1,
                                         m_matchIgnoredPattern);

    distributionControlStubFactory.assertNoMoreCalls();

    assertNotNull(fileDistribution.getAgentCacheState());

    final File anotherFile = new File(getDirectory(), "foo");
    assertTrue(anotherFile.mkdir());
    final Directory directory2 = new Directory(anotherFile);

    final FileDistributionHandler fileDistributionHandler1 =
      fileDistribution.getHandler();

    distributionControlStubFactory.assertNoMoreCalls();

    // Test with same directory.
    final FileDistributionHandler fileDistributionHandler2 =
      fileDistribution.getHandler();

    assertNotSame(fileDistributionHandler1, fileDistributionHandler2);
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with a different directory.
    fileDistribution.setDirectory(directory2);

    final FileDistributionHandler fileDistributionHandler3 =
      fileDistribution.getHandler();

    assertNotSame(fileDistributionHandler1, fileDistributionHandler3);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler3);

    distributionControlStubFactory.assertNoMoreCalls();
  }

  public void testScanDistributionFiles() throws Exception {
    final RandomStubFactory<DistributionControl>
      distributionControlStubFactory =
        RandomStubFactory.create(DistributionControl.class);

    final UpdateableAgentCacheStateStubFactory
      agentCacheStateStubFactory =
        new UpdateableAgentCacheStateStubFactory();

    final UpdateableAgentCacheState agentCacheState =
      agentCacheStateStubFactory.getStub();

    final RandomStubFactory<FileChangedListener> fileListenerStubFactory =
      RandomStubFactory.create(FileChangedListener.class);

    final Directory directory = new Directory(getDirectory());
    agentCacheStateStubFactory.override_setDirectory(null, directory);
    agentCacheStateStubFactory.override_setFileFilterPattern(
      null, m_matchIgnoredPattern);

    final FileDistributionImplementation fileDistribution =
      new FileDistributionImplementation(
        distributionControlStubFactory.getStub(),
        agentCacheState);
    fileDistribution.addFileChangedListener(fileListenerStubFactory.getStub());

    fileDistribution.scanDistributionFiles();

    final CallData filesChangedCall =
      fileListenerStubFactory.assertSuccess("filesChanged", File[].class);
    final File[] changedFiles = (File[])(filesChangedCall.getParameters()[0]);
    assertEquals(1, changedFiles.length);
    assertTrue(changedFiles[0].equals(directory.getFile()));

    fileListenerStubFactory.assertNoMoreCalls();

    final File file1 = new File(getDirectory(), "file1");
    assertTrue(file1.createNewFile());
    final File file2 = new File(getDirectory(), "file2");
    assertTrue(file2.createNewFile());
    final File oldFile = new File(getDirectory(), "file3");
    assertTrue(oldFile.createNewFile());
    assertTrue(oldFile.setLastModified(0));
    assertTrue(file2.setLastModified(file1.lastModified() + 5000));

    assertTrue(file1.delete());
    assertTrue(file1.createNewFile());
    assertTrue(file2.delete());
    assertTrue(file2.createNewFile());
    assertTrue(file2.setLastModified(file1.lastModified() + 5000));

    fileDistribution.scanDistributionFiles();
    assertEquals(file1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    final CallData filesChangedCall2 =
      fileListenerStubFactory.assertSuccess("filesChanged", File[].class);
    final File[] changedFiles2 = (File[])(filesChangedCall2.getParameters()[0]);
    assertEquals(3, changedFiles2.length);
    AssertUtilities.assertArrayContainsAll(
      changedFiles2,
      new File[] { directory.getFile(), file1, file2 } );

    fileListenerStubFactory.assertNoMoreCalls();

    // Even if the cache has older out of date times, we only scan from the
    // last scan time.
    final File file4 = new File(getDirectory(), "file4");
    assertTrue(file4.createNewFile());
    agentCacheStateStubFactory.resetOutOfDate();
    fileDistribution.scanDistributionFiles();
    assertEquals(file4.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());
    fileListenerStubFactory.resetCallHistory();

    fileDistribution.setDirectory(directory);
    fileDistribution.setFileFilterPattern(m_matchAllPattern);
    fileDistribution.scanDistributionFiles();
    fileListenerStubFactory.assertNoMoreCalls();

    // Do some checks with directories
    agentCacheStateStubFactory.resetOutOfDate();
    final File testDirectory = new File(getDirectory(), "test");
    assertTrue(testDirectory.mkdir());
    fileDistribution.setDirectory(new Directory(testDirectory));

    // Set the fileDistribution scan time to now.
    fileDistribution.setFileFilterPattern(m_matchIgnoredPattern);
    fileDistribution.scanDistributionFiles();
    fileListenerStubFactory.resetCallHistory();

    assertTrue(testDirectory.setLastModified(
      testDirectory.lastModified() + 5000));

    final File directory1 = new File(getDirectory(), "test/dir1");
    assertTrue(directory1.mkdir());
    final File oldDirectory = new File(getDirectory(), "test/dir3");
    assertTrue(oldDirectory.mkdir());
    final File directory2 = new File(getDirectory(), "test/dir3/dir2");
    assertTrue(directory2.mkdir());
    assertTrue(oldDirectory.setLastModified(0));
    assertTrue(file2.setLastModified(file1.lastModified() + 5000));

    fileDistribution.scanDistributionFiles();
    // Directories no longer affect cache.
    assertEquals(Long.MAX_VALUE,
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    final CallData directoriesChangedCall =
      fileListenerStubFactory.assertSuccess("filesChanged", File[].class);
    final File[] changedDirectories =
      (File[])(directoriesChangedCall.getParameters()[0]);
    assertEquals(3, changedDirectories.length);
    AssertUtilities.assertArrayContainsAll(changedDirectories,
      new File[] { testDirectory, directory1, directory2 } );

    fileListenerStubFactory.assertNoMoreCalls();

    // If the cache has been reset, we scan the lot.
    fileDistribution.setDirectory(directory);
    fileDistribution.scanDistributionFiles();
    assertEquals(0, agentCacheStateStubFactory.getEarliestOutOfDateTime());
    fileListenerStubFactory.resetCallHistory();

    // Test with r/o directory, just for coverage's sake.
    final Directory subdirectory =
      new Directory(new File(getDirectory(), "subdirectory"));
    subdirectory.create();
    final File f1 = new File(subdirectory.getFile(), "file");
    assertTrue(f1.createNewFile());

    FileUtilities.setCanAccess(subdirectory.getFile(), false);

    fileDistribution.setDirectory(subdirectory);
    fileDistribution.scanDistributionFiles();

    assertEquals(f1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    FileUtilities.setCanAccess(subdirectory.getFile(), true);
  }

  public static class UpdateableAgentCacheStateStubFactory
    extends RandomStubFactory<UpdateableAgentCacheState> {

    private long m_earliestOutOfDateTime = Long.MAX_VALUE;
    private Directory m_directory;
    private Pattern m_pattern;

    public UpdateableAgentCacheStateStubFactory() {
      super(UpdateableAgentCacheState.class);
    }

    public long getEarliestOutOfDateTime() {
      return m_earliestOutOfDateTime;
    }

    public void override_setNewFileTime(Object proxy, long t) {
      if (t < m_earliestOutOfDateTime) {
        m_earliestOutOfDateTime = t;
      }
    }

    public void resetOutOfDate() {
      m_earliestOutOfDateTime = Long.MAX_VALUE;
    }

    public void override_setDirectory(Object proxy, Directory directory) {
       m_directory = directory;
    }

    public void override_setFileFilterPattern(Object proxy,
                                              Pattern fileFilterPattern) {
      m_pattern = fileFilterPattern;
    }

    public CacheParameters override_getCacheParameters(Object proxy) {
      return new CacheParametersImplementation(m_directory,
                                               m_pattern);
    }
  }

  public void testFilter() throws Exception {
    final Pattern pattern = Pattern.compile("^a.*[^/]$|.*exclude.*|.*b/$");

    final FileFilter filter =
      new FileDistributionImplementation.FixedPatternFileFilter(10000L,
                                                                pattern);

    final String[] acceptableFilenames = new String[] {
      "DoesntStartWithA.acceptable",
      "blah blah blah",
      "blah-file-store",
    };

    for (int i = 0; i < acceptableFilenames.length; ++i) {
      final File f = new File(getDirectory(), acceptableFilenames[i]);
      assertTrue(f.createNewFile());
      assertTrue(f.getPath() + " is acceptable", filter.accept(f));
    }

    final String[] unacceptableFileNames = new String[] {
      "exclude me",
      "a file beginning with a",
      "a directory ending with b",
    };

    for (int i = 0; i < unacceptableFileNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableFileNames[i]);
      assertTrue(f.createNewFile());

      assertTrue(f.getPath() + " is unacceptable", !filter.accept(f));
    }

    final File timeFile = new File(getDirectory(), "time file");
    assertTrue(timeFile.createNewFile());
    assertTrue(timeFile.getPath() + " is acceptable", filter.accept(timeFile));
    assertTrue(timeFile.setLastModified(123L));
    assertTrue(timeFile.getPath() + " is unacceptable",
               !filter.accept(timeFile));

    // Add an error margin, as Linux does not support setting the modification
    // date with millisecond precision.
    assertTrue(timeFile.setLastModified(101001L));
    assertTrue(timeFile.getPath() + " is acceptable", filter.accept(timeFile));

    final String[] acceptableDirectoryNames = new String[] {
      "a directory ending with b.not",
      "include me",
    };

    for (int i = 0; i < acceptableDirectoryNames.length; ++i) {
      final File f = new File(getDirectory(), acceptableDirectoryNames[i]);
      assertTrue(f.mkdir());
      assertTrue(f.getPath() + " is acceptable", filter.accept(f));
    }

    final String[] unacceptableDirectoryNames = new String[] {
      "a directory ending with b",
      "exclude me",
    };

    for (int i = 0; i < unacceptableDirectoryNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableDirectoryNames[i]);
      assertTrue(f.getPath() + " is unacceptable", !filter.accept(f));
    }

    final File timeDirectory = new File(getDirectory(), "time directory");
    assertTrue(timeDirectory.mkdir());
    assertTrue(timeDirectory.getPath() + " is acceptable",
               filter.accept(timeDirectory));
    assertTrue(timeDirectory.setLastModified(123L));
    assertTrue(timeDirectory.getPath() + " is acceptable",
               filter.accept(timeDirectory));

    final File fileStoreDirectory = new File(getDirectory(), "foo-file-store");
    assertTrue(fileStoreDirectory.mkdir());
    assertTrue(fileStoreDirectory.getPath() + " is acceptable",
               filter.accept(fileStoreDirectory));

    final File readMeFile = new File(fileStoreDirectory, "README.txt");
    assertTrue(readMeFile.createNewFile());
    assertTrue(fileStoreDirectory.getPath() + " is unacceptable",
               !filter.accept(fileStoreDirectory));

    assertTrue(readMeFile.delete());
    assertTrue(fileStoreDirectory.getPath() + " is acceptable",
               filter.accept(fileStoreDirectory));
  }

  public void testIsDistributableFile() throws Exception {
    final Pattern pattern = Pattern.compile("^a.*[^/]$|.*exclude.*|.*b/$");

    final FileDistributionImplementation fileDistribution =
      new FileDistributionImplementation(
        null, m_processControl, new Directory(getDirectory()), pattern);
    final FileFilter filter = fileDistribution.getDistributionFileFilter();

    final String[] acceptableFilenames = new String[] {
      "DoesntStartWithA.acceptable",
      "blah blah blah",
      "blah-file-store",
    };

    for (int i = 0; i < acceptableFilenames.length; ++i) {
      final File f = new File(getDirectory(), acceptableFilenames[i]);
      assertTrue(f.createNewFile());
      assertTrue(f.getPath() + " is distributable", filter.accept(f));
    }

    final String[] unacceptableFileNames = new String[] {
      "exclude me",
      "a file beginning with a",
      "a directory ending with b",
    };

    for (int i = 0; i < unacceptableFileNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableFileNames[i]);
      assertTrue(f.createNewFile());

      assertTrue(f.getPath() + " is not distributable", !filter.accept(f));
    }

    // filter should still be valid if pattern changes.
    fileDistribution.setFileFilterPattern(Pattern.compile(".*exclude.*"));

    assertTrue(!filter.accept(new File(getDirectory(), "exclude me")));
    assertTrue(
      filter.accept(new File(getDirectory(), "a file begining with a")));

  }
}
