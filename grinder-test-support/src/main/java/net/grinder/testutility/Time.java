// Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006 Philip Aston
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


/**
 * Abstract base class which times a method and returns whether it
 * executed within the given range.
 *
 * @author Philip Aston
 */
public abstract class Time {

  /**
   * We slacken assertions about time to account for precision of J2SE time API.
   *
   * On JRockit 1.4.2_10 and a Dell 520, I've seen this code:
   *
   *   final long t1 = System.currentTimeMillis();
   *   Thread.sleep(50);
   *   System.out.println(System.currentTimeMillis()-t1);
   *
   * print out "47". I've repeated this with Thread.sleep(50, 0).
   * Javadoc for System.currentTimeMillis() mentions its inaccuracy, and I think
   * it's this method rather than Thread.sleep() that's so slack. Can't use
   * System.nanoTime() as we can't assume J2SE 1.5.
   *
   * Value obtained empirically.
   */
  public static final long J2SE_TIME_ACCURACY_MILLIS = 10;

  private final long m_expectedMin;
  private final long m_expectedMax;

  public Time(long expectedMin, long expectedMax) {
    m_expectedMin = expectedMin - J2SE_TIME_ACCURACY_MILLIS;
    m_expectedMax = expectedMax + J2SE_TIME_ACCURACY_MILLIS;
  }

  public abstract void doIt() throws Exception;

  public boolean run() throws Exception {
    final long then = System.currentTimeMillis();
    doIt();
    final long time = System.currentTimeMillis() - then;

    if (m_expectedMin > time) {
      // We never expect this - it means our margin is too small. Print out
      // to aid debug.
      System.err.println("Expected < " + m_expectedMin + " but was " + time);
    }

    return m_expectedMin <= time && m_expectedMax >= time;
  }
}
