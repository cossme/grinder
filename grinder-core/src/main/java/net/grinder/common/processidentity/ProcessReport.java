// Copyright (C) 2005 - 2012 Philip Aston
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

package net.grinder.common.processidentity;

import java.util.Comparator;


/**
 * Common interface for enquiring about a process.
 *
 * @author Philip Aston
 */
public interface ProcessReport {

  /** Process state. */
  enum State {
    /** Process is started. */
    STARTED,

    /** Process is running. */
    RUNNING,

    /** Process is finished. */
    FINISHED,

    /** Process status is unknown. */
    UNKNOWN;
  }

  /**
   * Return the unique process address.
   *
   * @return The process identity.
   */
  ProcessAddress<? extends ProcessIdentity> getProcessAddress();

  /**
   * Return the process status.
   *
   * @return The state.
   */
  State getState();

  /**
   * Comparator that compares ProcessReports by state, then by name.
   */
  final class StateThenNameThenNumberComparator
    implements Comparator<ProcessReport> {

    @Override public int compare(ProcessReport processReport1,
                                 ProcessReport processReport2) {

      final int stateComparison =
        processReport1.getState().compareTo(processReport2.getState());

      if (stateComparison == 0) {
        final ProcessIdentity identity1 =
          processReport1.getProcessAddress().getIdentity();

        final ProcessIdentity identity2 =
          processReport2.getProcessAddress().getIdentity();

        final int nameComparison =
          identity1.getName().compareTo(identity2.getName());

        if (nameComparison == 0) {
          return identity1.getNumber() - identity2.getNumber();
        }
        else {
          return nameComparison;
        }
      }
      else {
        return stateComparison;
      }
    }
  }
}
