// Copyright (C) 2012 - 2013 Philip Aston
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

import java.net.MalformedURLException;
import java.util.List;

import org.junit.runners.model.InitializationError;

/**
 * JUnit runner for Jython 2.5 and 2.7 installations.
 *
 * <p>
 * To use, annotate the test class with {@code @RunWith(Jython25Runner)}. A new
 * classloader will be used for each test.
 * </p>
 *
 * @author Philip Aston
 */
public class Jython25_27Runner extends JythonVersionRunner {

  private static final List<String> HOMES = getHomes(
    "jython2_5_3.dir",
    "jython2_5_2.dir",
    "jython2_5_1.dir",
    "jython2_5_0.dir",
    "jython2_5_4rc1.dir",
    "jython2_7b1.dir");

  public Jython25_27Runner(final Class<?> testClass)
      throws InitializationError, ClassNotFoundException, MalformedURLException {
    super(testClass, HOMES);
  }
}
