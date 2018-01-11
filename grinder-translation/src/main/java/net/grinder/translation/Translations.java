// Copyright (C) 2013 Philip Aston
// All rights reserved.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.translation;

import java.util.Locale;

/**
 * Java API to translation service.
 *
 * @author Philip Aston
 */
public interface Translations {

  /**
   * Obtain translations for a particular locale.
   *
   * @author Philip Aston
   */
  public interface Source {

    /**
     * Get translations.
     *
     * @param locale The locale.
     * @return The translations.
     */
    Translations getTranslations(Locale locale);
  }

  /**
   * Return a translation.
   *
   * @param key
   *          A key that identifies the translation.
   * @param formatArguments
   *          The translated string is interpolated with these optional
   *          {@link java.text.MessageFormat#format(java.lang.Object[],
   *          java.lang.StringBuffer, java.text.FieldPosition)
   *          format} arguments.
   * @return The translation.
   */
  String translate(String key, Object... formatArguments);
}
