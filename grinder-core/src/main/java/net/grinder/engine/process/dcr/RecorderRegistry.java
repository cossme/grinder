// Copyright (C) 2009 - 2011 Philip Aston
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

package net.grinder.engine.process.dcr;

import net.grinder.scriptengine.Recorder;


/**
 * Recorder registry.
 *
 * @author Philip Aston
 */
interface RecorderRegistry {

  /**
   * Registration method.
   *
   * @param target
   *          The target reference.
   * @param location
   *          String that uniquely identifies the instrumentation location.
   * @param recorder
   *          The recorder to register.
   */
  void register(Object target, String location, Recorder recorder);


  /**
   * Registration method for invocations that have two target references.
   *
   * @param target
   *          The target reference.
   * @param target
   *          The second target reference.
   * @param location
   *          String that uniquely identifies the instrumentation location.
   * @param recorder
   *          The recorder to register.
   */
  void register(Object target,
                Object target2,
                String location,
                Recorder recorder);
}
