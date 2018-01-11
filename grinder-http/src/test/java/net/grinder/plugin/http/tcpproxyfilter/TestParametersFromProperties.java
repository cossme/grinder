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

package net.grinder.plugin.http.tcpproxyfilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ParametersFromProperties}.
 *
 * @author Philip Aston
 */
public class TestParametersFromProperties {

  @Test
  public void testIsCommonHeader() {

    final HTTPRecordingParameters parameters = new ParametersFromProperties();

    assertTrue(parameters.isCommonHeader("Accept"));
    assertTrue(parameters.isCommonHeader("User-Agent"));
    assertTrue(parameters.isCommonHeader("faces-request"));

    assertFalse(parameters.isCommonHeader("If-None-Match"));
    assertFalse(parameters.isCommonHeader("Content-Type"));
    assertFalse(parameters.isCommonHeader("Foo"));
  }

  @Test
  public void testIsMirroredHeader() {

    final HTTPRecordingParameters parameters = new ParametersFromProperties();

    assertTrue(parameters.isMirroredHeader("Accept"));
    assertTrue(parameters.isMirroredHeader("User-Agent"));
    assertTrue(parameters.isMirroredHeader("If-None-Match"));
  }

  @Test
  public void testIsAdditionalHeaders() {

    System.setProperty("HTTPPlugin.additionalHeaders", "Foo ,bah");

    final HTTPRecordingParameters parameters = new ParametersFromProperties();

    assertTrue(parameters.isCommonHeader("Foo"));
    assertTrue(parameters.isCommonHeader("bah"));
    assertTrue(parameters.isMirroredHeader("Foo"));
    assertTrue(parameters.isMirroredHeader("bah"));

    assertFalse(parameters.isCommonHeader("Foo "));
    assertFalse(parameters.isCommonHeader("Bah"));
    assertFalse(parameters.isMirroredHeader("Foo "));
    assertFalse(parameters.isMirroredHeader("Bah"));

    System.clearProperty("HTTPPlugin.additionalHeaders");

  }
}
