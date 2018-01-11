// Copyright (C) 2006 Philip Aston
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


/**
 * Parse attribute strings such as those from HTML tokens into a map of name
 * value pairs.
 *
 * <p>The parser is relaxed, if it finds a partial map it will just ignore
 * it. The case of the key string is ignored. Where duplicate keys exist,
 * the value will be taken from the last one.</p>
 *
 * @author Philip Aston
 */
public interface AttributeStringParser {

  /**
   * Do the parse.
   *
   * @param string String to parse.
   * @return Parse result.
   */
  AttributeMap parse(String string);

  /**
   * Simple map that holds the result of the parse.
   */
  interface AttributeMap {

    /**
     * Get a value from the map. The case of the <code>key</code> is ignored.
     *
     * @param key
     *          The key
     * @return The value, or <code>null</code> if there is no value for the
     *         given key.
     */
    String get(String key);
  }
}
