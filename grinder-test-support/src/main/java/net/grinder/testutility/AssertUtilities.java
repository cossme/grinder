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

package net.grinder.testutility;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;



//import HTTPClient.NVPair;


/**
 * Stuff missing from JUnit.
 *
 * @author    Philip Aston
 */
public class AssertUtilities extends Assert {

  public static void assertArrayContainsAll(Object[] array1, Object[] array2) {
    final List<Object> list1 = Arrays.asList(array1);
    final List<Object> list2 = Arrays.asList(array2);
    assertTrue("" + list1 + " contains " + list2,
      new HashSet<Object>(list1).containsAll(list2));
  }

  public static void assertStartsWith(String text, String value) {
    assertTrue("'" + text + "' starts with '" + value + "'",
               text != null && text.indexOf(value) == 0);
  }

  public static void assertContains(String text, String value) {
    assertTrue("'" + text + "' contains '" + value + "'",
               text != null && text.indexOf(value) >= 0);
  }

  public static void assertEndsWith(String text, String value) {
    assertEquals("'" + text + "' ends with '" + value + "'",
                 text.length() - value.length(),
                 text.lastIndexOf(value));
  }

  public static void assertContainsHeader(String text,
                                          String key,
                                          String value) {

    final Pattern headerPattern =
      Pattern.compile(key + ":[ \t]*" + value + "\r\n");
    final Matcher matcher = headerPattern.matcher(text);

    assertTrue("'" + text + "' contains header '" + key + "' with value '" +
               value + "'",
               matcher.find());
  }

  public static void assertContainsPattern(String text, String pattern) {

    final Matcher matcher = Pattern.compile(pattern).matcher(text);

    assertTrue("'" + text + "' contains pattern '" + pattern + "'",
               matcher.find());
  }

  public static final <T> Set<T> asSet(final T... a) {
    return asSet(asList(a));
  }

  public static final <T> Set<T> asSet(final Collection<T> list) {
    return new HashSet<T>(list);
  }
}
