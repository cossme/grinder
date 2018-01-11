// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.console.common;

import java.util.Map;

import javax.swing.ImageIcon;


public class StubResources<T> implements Resources {
  private final Map<String, T> m_results;

  public StubResources(Map<String, T> results) {
    m_results = results;
  }

  public ImageIcon getImageIcon(String key) {
    return (ImageIcon) m_results.get(key);
  }

  public ImageIcon getImageIcon(String key, boolean warnIfMissing) {
    throw new AssertionError("not implemented");
  }

  public String getString(String key) {
    return (String) m_results.get(key);
  }

  public String getString(String key, boolean warnIfMissing) {
    return (String) m_results.get(key);
  }

  public String getStringFromFile(String key, boolean warnIfMissing) {
    throw new AssertionError("not implemented");
  }

  public void put(String key, T value) {
    m_results.put(key, value);
  }
}
