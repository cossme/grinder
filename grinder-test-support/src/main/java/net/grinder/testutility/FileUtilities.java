// Copyright (C) 2004 - 2011 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

package net.grinder.testutility;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * File utilities used by the unit tests.
 *
 * @author Philip Aston
 */
public class FileUtilities {

  private static Random s_random  = new Random();

  public static void setCanAccess(File file, boolean canAccess)
    throws Exception {

    if(System.getProperty("os.name").startsWith("Windows")) {
      // Strewth: getCanonicalPath doesn't quote spaces correctly for cacls.
      String path = file.getCanonicalPath();
      path = path.replaceAll("%20", " ");

      // Sadly cygwin ntsec support doesn't allow us to ignore inherited
      // attributes. Do this instead:
      exec(new String[] {
            "cacls",
            path,
            "/E",
            "/P",
            System.getProperty("user.name") + ":" + (canAccess ? "F" : "N"),
           });
    }
    else {
      // Assume UNIX.
      exec(new String[] {
            "chmod",
            canAccess ? "ugo+rwx" : "ugo-rwx",
            file.getCanonicalPath(),
           });
    }
  }

  private static void exec(String[] command)
    throws InterruptedException {

    final Process process;

    try {
      process = Runtime.getRuntime().exec(command);
    }
    catch (IOException e) {
      throw new RuntimeException(
        "Couldn't chmod: perhaps you should patch this" +
        "test for your platform?",
        e) {};
    }

    process.waitFor();

    assertEquals("exec of " + Arrays.asList(command) +
      " succeeded", 0, process.exitValue());
  }

  public static String readLastLine(File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));

    try {
      String last = null;

      while (true) {
        final String line = reader.readLine();
        if (line == null) {
          return last;
        }

        last = line;
      }
    }
    finally {
      reader.close();
    }
  }

  public static int countLines(File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));

    try {
      int result = 0;

      while (true) {
        final String line = reader.readLine();
        if (line == null) {
          return result;
        }

        ++result;
      }
    }
    finally {
      reader.close();
    }
  }

  public static void createRandomFile(File file) throws IOException {
    file.getParentFile().mkdirs();

    final OutputStream out = new FileOutputStream(file);
    final byte[] bytes = new byte[s_random.nextInt(2000)];
    s_random.nextBytes(bytes);
    out.write(bytes);
    out.close();
  }

  public static String fileContents(File file) throws IOException {

    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    new StreamCopier().copy(new FileInputStream(file), byteOutputStream);

    return new String(byteOutputStream.toByteArray());
  }

  public static void createFile(File file, List<String> lines)
    throws IOException {

    file.getParentFile().mkdirs();
    file.createNewFile();

    final PrintWriter out = new PrintWriter(new FileWriter(file));

    try {
      for (String line : lines) {
        out.println(line);
      }
    }
    finally {
      out.close();
    }
  }

  public static void createFile(File file, String... lines) throws IOException {
    createFile(file, asList(lines));
  }
}
