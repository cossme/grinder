// Copyright (C) 2005 - 2010 Philip Aston
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

package net.grinder.console.service;

import net.grinder.common.Test;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorQueue;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import org.picocontainer.Startable;
import org.slf4j.Logger;

import java.util.Set;

public class Bootstrap implements Startable {

    private static Bootstrap INSTANCE;

    protected ErrorQueue errorQueue;
    protected ConsoleProperties consoleProperties;
    protected final SampleModel model;
    protected final SampleModelViews sampleModelViews;
    protected ProcessControl processControl;
    protected FileDistribution fileDistribution;
    protected ProcessControl.ProcessReports[] m_processReports;
    protected ModelTestIndex testIndex = null;

    protected Logger logger;

    public static Bootstrap getInstance() {
        return INSTANCE;
    }

    public static ConsoleProperties getConsoleProperties () {
        if (INSTANCE != null) {
            return INSTANCE.consoleProperties;
        }
        else {
            return null;
        }
    }

    public Bootstrap(ConsoleProperties consoleProperties,
                     SampleModel modelParam,
                     SampleModelViews sampleModelViewsParam,
                     ProcessControl processControl,
                     ErrorQueue errorQueue,
                     FileDistribution fileDistribution)
            throws ConsoleException    {
        try {
            this.errorQueue = errorQueue;
            this.consoleProperties = consoleProperties;
            this.model = modelParam;
            this.sampleModelViews = sampleModelViewsParam;
            this.processControl = processControl;
            this.fileDistribution = fileDistribution;

            this.processControl.addProcessStatusListener(
                    new ProcessControl.Listener() {
                        public void update(ProcessControl.ProcessReports[] processReports) {
                            m_processReports = processReports;
                        }
                    }
            );

            this.model.addModelListener(new SampleModel.AbstractListener() {
                public void newTests(Set<Test> newTests, ModelTestIndex modelTestIndex) {
                    testIndex = modelTestIndex;
                }
            });
            INSTANCE=this;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ConsoleException(e.getMessage());
        }
    }


    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public ProcessControl getProcessControl() {
        return processControl;
    }

    public SampleModelViews getSampleModelViews() {
        return sampleModelViews;
    }

    public SampleModel getModel() {
        return model;
    }
}