// Copyright (C) 2004 - 2012 Philip Aston
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.grinder.common.Closer;


/**
 * Wrapper around a directory path that behaves in a similar manner to
 * <code>java.io.File</code>. Provides utility methods for working
 * with the directory represented by the path.
 *
 * <p>A <code>Directory</code> be constructed with a path that
 * represents an existing directory, or a path that represents no
 * existing file. The physical directory can be created later using
 * {@link #create}.</p>
 *
 * @author Philip Aston
 */
public final class Directory implements Serializable {
  private static final long serialVersionUID = 1;

  private static final FileFilter s_matchAllFilesFilter =
    new MatchAllFilesFilter();

  private final File m_directory;
  private final List<String> m_warnings = new ArrayList<String>();

  /**
   * Returns a filter matching all files.
   *
   * @return A filter matching all files.
   */
  public static FileFilter getMatchAllFilesFilter() {
    return s_matchAllFilesFilter;
  }

  /**
   * Constructor that builds a Directory for the current working directory.
   */
  public Directory() {
    m_directory = new File(".");
  }

  /**
   * Constructor.
   *
   * @param directory The directory path upon which this
   * <code>Directory</code> operates.
   * @exception DirectoryException If the path <code>directory</code>
   * represents a file that exists but is not a directory.
   */
  public Directory(final File directory) throws DirectoryException {
    if (directory == null) {
      m_directory = new File(".");
    }
    else {
      if (directory.exists() && !directory.isDirectory()) {
        throw new DirectoryException(
          "'" + directory.getPath() + "' is not a directory");
      }

      m_directory = directory;
    }
  }

  /**
   * Create the directory if it doesn't exist.
   *
   * @exception DirectoryException If the directory could not be created.
   */
  public void create() throws DirectoryException {
    if (!getFile().exists()) {
      if (!getFile().mkdirs()) {
        throw new DirectoryException(
          "Could not create directory '" + getFile() + "'");
      }
    }
  }

  /**
   * Get as a {@link File}.
   *
   * @return The <code>File</code>.
   */
  public File getFile() {
    return m_directory;
  }

  /**
   * Return a {@link File} representing the absolute path of a file in this
   * directory.
   *
   * @param child
   *            Relative file in this directory. If <code>null</code>, the
   *            result is equivalent to {@link #getFile()}.
   * @return The <code>File</code>.
   */
  public File getFile(final File child) {
    if (child == null) {
      return getFile();
    }
    else {
      return new File(getFile(), child.getPath());
    }
  }

  /**
   * Equivalent to <code>listContents(filter, false, false)</code>.
   *
   * @param filter
   *          Filter that controls the files that are returned.
   * @return The list of files. Files are relative to the directory, not
   *         absolute. More deeply nested files are later in the list. The list
   *         is empty if the directory does not exist.
   * @see #listContents(FileFilter, boolean, boolean)
   */
  public File[] listContents(final FileFilter filter) {
    return listContents(filter, false, false);
  }

  /**
   * List the files in the hierarchy below the directory that have been modified
   * after <code>since</code>.
   *
   * @param filter
   *          Filter that controls the files that are returned.
   * @param includeDirectories
   *          Whether to include directories in the returned files. Only
   *          directories that match the filter will be returned.
   * @param absolutePaths
   *          Whether returned files should be relative to the directory or
   *          absolute.
   * @return The list of files. More deeply nested files are later in the list.
   *         The list is empty if the directory does not exist.
   */
  public File[] listContents(final FileFilter filter,
                             final boolean includeDirectories,
                             final boolean absolutePaths) {

    final List<File> resultList = new ArrayList<File>();
    final Set<File> visited = new HashSet<File>();
    final List<File> directoriesToVisit = new ArrayList<File>();

    final File rootFile = getFile();

    if (rootFile.exists() && filter.accept(rootFile)) {

      // We use null here rather than File("") as it helps below. File(File(""),
      // "blah") is "/blah", but File(null, "blah") is "blah".
      directoriesToVisit.add(null);

      if (includeDirectories) {
        resultList.add(absolutePaths ? rootFile : new File(""));
      }
    }

    while (directoriesToVisit.size() > 0) {
      final File[] directories =
        directoriesToVisit.toArray(new File[directoriesToVisit.size()]);

      directoriesToVisit.clear();

      for (final File relativeDirectory : directories) {
        final File absoluteDirectory = getFile(relativeDirectory);

        visited.add(relativeDirectory);

        // We use list() rather than listFiles() so the results are
        // relative, not absolute.
        final String[] children = absoluteDirectory.list();

        if (children == null) {
          // This can happen if the user does not have permission to
          // list the directory.
          synchronized (m_warnings) {
            m_warnings.add("Could not list '" + absoluteDirectory);
          }
          continue;
        }

        for (final String element : children) {
          final File relativeChild = new File(relativeDirectory, element);
          final File absoluteChild = new File(absoluteDirectory, element);

          if (filter.accept(absoluteChild)) {
            // Links (hard or symbolic) are transparent to isFile(),
            // isDirectory(); but we're careful to filter things that are
            // neither (e.g. FIFOs).
            if (includeDirectories && absoluteChild.isDirectory() ||
                absoluteChild.isFile()) {
              resultList.add(absolutePaths ? absoluteChild : relativeChild);
            }

            if (absoluteChild.isDirectory() &&
                !visited.contains(relativeChild)) {
              directoriesToVisit.add(relativeChild);
            }
          }
        }
      }
    }

    return resultList.toArray(new File[resultList.size()]);
  }

  /**
   * Delete the contents of the directory.
   *
   * <p>Does nothing if the directory does not exist.</p>
   *
   * @throws DirectoryException If a file could not be deleted. The
   * contents of the directory are left in an indeterminate state.
   * @see #delete
   */
  public void deleteContents() throws DirectoryException {
    // We rely on the order of the listContents result: more deeply
    // nested files are later in the list.
    final File[] deleteList = listContents(s_matchAllFilesFilter, true, true);

    for (int i = deleteList.length - 1; i >= 0; --i) {
      if (deleteList[i].equals(getFile())) {
        continue;
      }

      if (!deleteList[i].delete()) {
        throw new DirectoryException(
          "Could not delete '" + deleteList[i] + "'");
      }
    }
  }

  /**
   * Delete the directory. This will fail if the directory is not
   * empty.
   *
   * @throws DirectoryException If the directory could not be deleted.
   * @see #deleteContents
   */
  public void delete() throws DirectoryException {
    if (!getFile().delete()) {
      throw new DirectoryException("Could not delete '" + getFile() + "'");
    }
  }

  /**
   * If possible, convert a path relative to the current working directory to
   * a path relative to this directory. If not possible, return the absolute
   * version of the path.
   *
   * @param file The input path. Relative to the CWD, or absolute.
   * @return The simplified path.
   */
  public File rebaseFromCWD(final File file) {
    final File absolute = file.getAbsoluteFile();
    final File relativeResult =  relativeFile(absolute, true);

    if (relativeResult == null) {
      return absolute;
    }

    return relativeResult;
  }

  /**
   * Convert the supplied {@code file}, to a path relative to this directory.
   * The file need not exist.
   *
   * <p>
   * If {@code file} is relative, this directory is considered its base.
   * </p>
   *
   * @param file
   *          The file to search for.
   * @param mustBeChild
   *          If {@code true} and {@code path} belongs to another file system;
   *          is absolute with a different base path; or is a relative path
   *          outside of the directory ({@code ../somewhere/else}), then
   *          {@code null} will be returned.
   * @return A path relative to this directory, or {@code null}.
   */
  public File relativeFile(final File file, final boolean mustBeChild) {

    final File f;

    if (file.isAbsolute()) {
      f = file;
    }
    else if (!mustBeChild) {
      return file;
    }
    else {
      f = getFile(file);
    }

    return relativePath(getFile(), f, mustBeChild);
  }

  /**
   * Calculate a relative path from {@code from} to {@code to}.
   *
   * <p>
   * Package scope for unit tests.
   * </p>
   *
   * @param from
   *          Source file or directory.
   * @param to
   *          Target file or directory.
   * @param mustBeChild
   *          If {@code true} and {@code to} is not a child of {@code from} or
   *          equivalent to {@from}, return {@code null}.
   * @return The relative path. {@code null} if {@code to} belongs to a
   *         different file system, or {@code mustBeChild} is {@code true} and
   *         {@code to} is not a child path of {@code from}.
   * @throws UnexpectedIOException
   *           If a canonical path could not be calculated.
   */
  static File relativePath(final File from,
                           final File to,
                           final boolean mustBeChild) {
    final String[] fromPaths;
    final String[] toPaths;

    try {
      fromPaths = splitPath(from.getCanonicalPath());
      toPaths = splitPath(to.getCanonicalFile().getPath());
    }
    catch (final IOException e) {
      throw new UnexpectedIOException(e);
    }

    int i = 0;

    while (i < fromPaths.length &&
           i < toPaths.length &&
           fromPaths[i].equals(toPaths[i])) {
      ++i;
    }

    if (mustBeChild && i != fromPaths.length) {
      return null;
    }

    // i == 0: The root file is different.
    // i == 1: The root file is the same, but there's no common path.
    if (i <= 1) {
      return null;
    }

    final StringBuilder result = new StringBuilder();

    for (int j = i; j < fromPaths.length; ++j) {
      result.append("..");
      result.append(File.separator);
    }

    for (int j = i; j < toPaths.length; ++j) {
      result.append(toPaths[j]);

      if (j != toPaths.length - 1) {
        result.append(File.separator);
      }
    }

    if (result.length() == 0) {
      return new File(".");
    }

    return new File(result.toString());
  }

  private static String[] splitPath(final String path) {
    return path.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
  }

  /**
   * Rebase a whole path by calling {@link #rebaseFile} on each of its
   * elements and joining the result.
   *
   * @param path
   *          The path.
   * @return The result.
   */
  public List<File> rebasePath(final String path) {
    final String[] elements = path.split(File.pathSeparator);

    final List<File> result = new ArrayList<File>(elements.length);

    for (final String e : elements) {
      if (!e.isEmpty()) {
        result.add(rebaseFromCWD(new File(e)));
      }
    }

    return result;
  }

  /**
   * Test whether a File represents the name of a file that is a descendant of
   * the directory.
   *
   * @param file File to test.
   * @return {@code boolean} => file is a descendant.
   */
  public boolean isParentOf(final File file) {
    final File thisFile = getFile();

    File candidate = file.getParentFile();

    while (candidate != null) {
      if (thisFile.equals(candidate)) {
        return true;
      }

      candidate = candidate.getParentFile();
    }


    return false;
  }

  /**
   * Copy contents of the directory to the target directory.
   *
   * @param target Target directory.
   * @param incremental <code>true</code> => copy newer files to the
   * directory. <code>false</code> => overwrite the target directory.
   * @throws IOException If a file could not be copied. The contents
   * of the target directory are left in an indeterminate state.
   */
  public void copyTo(final Directory target, final boolean incremental)
    throws IOException {

    if (!getFile().exists()) {
      throw new DirectoryException(
        "Source directory '" + getFile() + "' does not exist");
    }

    target.create();

    if (!incremental) {
      target.deleteContents();
    }

    final File[] files = listContents(s_matchAllFilesFilter, true, false);
    final StreamCopier streamCopier = new StreamCopier(4096, false);

    for (final File file : files) {
      final File source = getFile(file);
      final File destination = target.getFile(file);

      if (source.isDirectory()) {
        destination.mkdirs();
      }
      else {
        // Copy file.
        if (!incremental ||
            !destination.exists() ||
            source.lastModified() > destination.lastModified()) {

          FileInputStream in = null;
          FileOutputStream out = null;

          try {
            in = new FileInputStream(source);
            out = new FileOutputStream(destination);
            streamCopier.copy(in, out);
          }
          finally {
            Closer.close(in);
            Closer.close(out);
          }
        }
      }
    }
  }

  /**
   * Return a list of warnings that have occurred since the last time
   * {@link #getWarnings} was called.
   *
   * @return The list of warnings.
   */
  public String[] getWarnings() {
    synchronized (m_warnings) {
      try {
        return m_warnings.toArray(new String[m_warnings.size()]);
      }
      finally {
        m_warnings.clear();
      }
    }
  }

  /**
   * An exception type used to report <code>Directory</code> related
   * problems.
   */
  public static final class DirectoryException extends IOException {
    DirectoryException(final String message) {
      super(message);
    }
  }

  /**
   * Delegate equality to our <code>File</code>.
   *
   * @return The hash code.
   */
  @Override
  public int hashCode() {
    return getFile().hashCode();
  }

  /**
   * Delegate equality to our <code>File</code>.
   *
   * @param o Object to compare.
   * @return <code>true</code> => equal.
   */
  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || o.getClass() != Directory.class) {
      return false;
    }

    return getFile().equals(((Directory)o).getFile());
  }

  private static class MatchAllFilesFilter implements FileFilter {
    @Override
    public boolean accept(final File file) {
      return true;
    }
  }
}
