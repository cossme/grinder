; Copyright (C) 2012 - 2013 Philip Aston
; All rights reserved.
;
; This file is part of The Grinder software distribution. Refer to
; the file LICENSE which is part of The Grinder distribution for
; licensing details. The Grinder distribution is available on the
; Internet at http://grinder.sourceforge.net/
;
; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
; COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
; INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
; OF THE POSSIBILITY OF SUCH DAMAGE.

(ns net.grinder.console.service.bootstrap
  "A Bootstrap Java class that is registered as a dynamic console
   component in META-INF/net.grinder.console. At runtime, PicoContainer
   will supply the constructor with implementations of the declared
   parameters."

  (:gen-class
   :name net.grinder.console.service.Bootstrap
   :constructors { [net.grinder.console.model.ConsoleProperties
                    net.grinder.console.model.SampleModel
                    net.grinder.console.model.SampleModelViews
                    net.grinder.console.communication.ProcessControl
                    net.grinder.console.common.ErrorQueue
                    net.grinder.console.distribution.FileDistribution
                    net.grinder.console.common.Resources]
                  []
                   }
   :init init
   :implements [org.picocontainer.Startable]
   :state state
   :prefix bootstrap-
   :impl-ns net.grinder.console.service.bootstrap_impl
  ))

