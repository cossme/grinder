// Copyright (C) 2005 - 2011 Philip Aston
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * <p>
 * Create objects that unknown to the standard class loaders. Useful for testing
 * ClassNotFoundException handling.
 * </p>
 *
 * @author Philip Aston
 */
public class IsolatedObjectFactory {

  public static Class<?> getIsolatedObjectClass() {
    try {
      return new SimpleObjectClassLoader().loadClass("SimpleObject");
    }
    catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  public static Object getIsolatedObject() {
    try {
      return getIsolatedObjectClass().newInstance();
    }
    catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private static class SimpleObjectClassLoader extends ClassLoader {

    protected synchronized Class<?> loadClass(String name, boolean resolve)
      throws ClassNotFoundException {

      if (name.equals("SimpleObject")) {
        Class<?> c = findLoadedClass(name);

        if (c == null) {
          final InputStream resource =
            getParent().getResourceAsStream(
               getClass().getPackage().getName().replace('.', '/') +
              "/resources/SimpleObject.clazz");

          final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

          try {
            new StreamCopier().copy(resource, byteStream);
            final byte[] bytes = byteStream.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
          }
          catch (IOException e) {
            throw new ClassNotFoundException(name, e);
          }
        }

        if (resolve) {
          resolveClass(c);
        }

        return c;
      }

      return super.loadClass(name, resolve);
    }
  }
}
