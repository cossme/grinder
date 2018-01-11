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

package net.grinder.engine.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.process.WorkerProcessEntryPoint;
import net.grinder.util.Directory;


/**
 * Builds the worker process command line.
 *
 * @author Philip Aston
 */
final class WorkerProcessCommandLine implements CommandLine {

  private static final Pattern AGENT_JAR_FILENAME_PATTERN =
      Pattern.compile("^grinder-dcr-agent-([\\d.]*)(-.*)?.jar$");

  private final Directory m_workingDirectory;
  private final List<String> m_command;
  private final int m_commandClassIndex;

  public WorkerProcessCommandLine(final GrinderProperties properties,
                                  final Properties systemProperties,
                                  final String jvmArguments,
                                  final Directory workingDirectory) {

    m_workingDirectory = workingDirectory;
    m_command = new ArrayList<String>();
    m_command.add(properties.getProperty("grinder.jvm", "java"));

    final List<File> systemClasspath =
      workingDirectory.rebasePath(
        systemProperties.getProperty("java.class.path", ""));

    final File agent = findAgentJarFile(systemClasspath);

    if (agent != null) {
      m_command.add("-javaagent:" + agent);
    }

    if (jvmArguments != null) {
      // Really should allow whitespace to be escaped/quoted.
      final StringTokenizer tokenizer = new StringTokenizer(jvmArguments);

      while (tokenizer.hasMoreTokens()) {
        final String token = tokenizer.nextToken();

        m_command.add(token);
      }
    }

    // Relative paths in grinder.jvm.classpath are not altered, so
    // are evaluated based on the working directory.
    final String additionalClasspath =
      properties.getProperty("grinder.jvm.classpath");

    final StringBuilder classpath = new StringBuilder();

    if (additionalClasspath != null) {
      classpath.append(additionalClasspath);
    }

    for (final File f : systemClasspath) {
      if (classpath.length() > 0) {
        classpath.append(File.pathSeparatorChar);
      }

      classpath.append(f.getPath());
    }

    if (classpath.length() > 0) {
      m_command.add("-classpath");

      m_command.add(classpath.toString());
    }

    m_commandClassIndex = m_command.size();
    m_command.add(WorkerProcessEntryPoint.class.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override public Directory getWorkingDirectory() {
    return m_workingDirectory;
  }

  /**
   * {@inheritDoc}
   */
  @Override public List<String> getCommandList() {
    return m_command;
  }

  private static final Set<String> s_unquoted = new HashSet<String>() { {
      add("-classpath");
      add("-client");
      add("-cp");
      add("-jar");
      add("-server");
    } };

  @Override
  public String toString() {
    final String[] commandArray = getCommandList().toArray(new String[0]);

    final StringBuilder buffer = new StringBuilder(commandArray.length * 10);

    for (int j = 0; j < commandArray.length; ++j) {
      if (j != 0) {
        buffer.append(" ");
      }

      final boolean shouldQuote =
        j != 0 &&
        j != m_commandClassIndex &&
        !s_unquoted.contains(commandArray[j]);

      if (shouldQuote) {
        buffer.append("'");
      }

      buffer.append(commandArray[j]);

      if (shouldQuote) {
        buffer.append("'");
      }
    }

    return buffer.toString();
  }

  static boolean isAgentJar(final String name) {
    final Matcher matcher = AGENT_JAR_FILENAME_PATTERN.matcher(name);

    if (matcher.matches()) {
      final String maybeSnapshot = matcher.group(2);
      return maybeSnapshot == null || "-SNAPSHOT".equals(maybeSnapshot);
    }

    return false;
  }

  /**
   * Package scope for unit tests.
   *
   * @param systemClasspath The path to search.
   */
  static File findAgentJarFile(final List<File> systemClasspath) {
    for (final File pathEntry : systemClasspath) {
      final File f = pathEntry.getParentFile();
      final File parentFile = f != null ? f : new File(".");

      final File[] children = parentFile.listFiles();

      if (children != null) {
        for (final File candidate : children) {
          if (isAgentJar(candidate.getName())) {
            return candidate;
          }
        }
      }
    }

    return null;
  }
}
