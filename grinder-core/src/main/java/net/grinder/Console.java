// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2013 Philip Aston
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

package net.grinder;

import java.util.Locale;

import net.grinder.common.GrinderException;
import net.grinder.console.ConsoleFoundation;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.translation.Translations;
import net.grinder.translation.impl.TranslationsSource;
import net.grinder.util.AbstractMainClass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Philip Aston
 */
public final class Console extends AbstractMainClass {

  private static final String USAGE =
    "  java " + Console.class.getName() + " [-headless]" +
    "\n" +
    "\n  -headless                    Don't use a graphical user interface.";

  private final ConsoleFoundation m_consoleFoundation;

  private Console(final String[] args,
                  final Resources resources,
                  final Translations translations,
                  final Logger logger)
    throws GrinderException {

    super(logger, USAGE);

    boolean headless = false;

    for (final String arg : args) {
      if ("-headless".equalsIgnoreCase(arg)) {
        headless = true;
      }
      else {
        throw barfUsage();
      }
    }

    m_consoleFoundation =
      new ConsoleFoundation(resources, translations, logger, headless);
  }

  private void run() {
    m_consoleFoundation.run();
  }

  /**
   * Entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(final String[] args)  {
    final Resources resources =
        new ResourcesImplementation(ConsoleFoundation.RESOURCE_BUNDLE);

    final Translations translations =
        new TranslationsSource().getTranslations(Locale.getDefault());

    final Logger logger =
      LoggerFactory.getLogger(translations.translate("console/terminal-label"));

    try {
      final Console console =
        new Console(args, resources, translations, logger);
      console.run();
    }
    catch (final LoggedInitialisationException e) {
      System.exit(1);
    }
    catch (final GrinderException e) {
      logger.error("Could not initialise", e);
      System.exit(2);
    }

    System.exit(0);
  }
}
