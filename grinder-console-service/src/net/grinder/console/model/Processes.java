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

package net.grinder.console.model;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.service.Bootstrap;
import net.grinder.util.Directory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by solcyr on 28/01/2018.
 */
public class Processes {

    ProcessControl.ProcessReports[] lastReport;
    ProcessControl pc = null;

    public void init() {
        if (pc == null && Bootstrap.getInstance() != null) {
            this.pc = Bootstrap.getInstance().getProcessControl();
            this.pc.addProcessStatusListener(new ProcessControl.Listener() {
                @Override
                public void update(ProcessControl.ProcessReports[] processReports) {
                    lastReport = processReports;
                }
            });
        }
    }

    /**
     * Return a vector containing the known status of all connected agents and
     * worker processes.
     */
    public List<Report> status() {
        init();
        List<Report> result = new ArrayList<>();
        if (lastReport != null) {
            for (int i = 0; i < lastReport.length; ++i) {
                result.add(i, toAgentAndWorkers(lastReport[i]));
            }
        }
        return result;
    }

    /**
     * Send a start signal to the agents to start worker processes.
     * This will only take effect if the agent is waiting for the start signal.
     * The agent will ignore start signals received while the workers are running.
     * We should revisit this in the future to allow process ramp up and ramp
     * down to be scripted.
     * The supplied-properties contain additional properties to pass on to the
     * agent. These take precedence over any specified by the console properties
     * \"propertiesFile\" attribute.
     */
    public String startWorkers(ConsoleProperties cp, Map<String, String> suppliedProperties) {
        init();
        File f = cp.getPropertiesFile();
        Directory directory = cp.getDistributionDirectory();
        GrinderProperties properties = new GrinderProperties();
        if (f != null && f.exists()) {
            try {
                properties = new GrinderProperties(f);
            } catch (GrinderProperties.PersistenceException e) {
                e.printStackTrace();
                return "error";
            }
        }
        intoGrinderProperties(properties, suppliedProperties);
        if (f != null && f.exists()) {
            try {
                pc.startWorkerProcessesWithDistributedFiles(directory, properties);
            } catch (ConsoleException e) {
                e.printStackTrace();
                return "error";
            }
        }
        else {
            pc.startWorkerProcesses(properties);
        }
        return "success";
    }

    private void intoGrinderProperties(GrinderProperties properties, Map<String, String> suppliedProperties) {
        if (suppliedProperties != null) {
            for (String key: suppliedProperties.keySet()) {
                properties.setProperty(key, suppliedProperties.get(key));
            }
        }
    }

    /**
     * Send a stop signal to connected worker processes.
     */
    public String stopWorkers() {
        init();
        pc.resetWorkerProcesses();
        return "success";
    }

    /**
     * Stop the agents, and their workers.
     */
    public String stopAgents() {
        init();
        pc.stopAgentAndWorkerProcesses();
        return "success";
    }

    private Report toAgentAndWorkers(ProcessControl.ProcessReports report) {
        Report agent = new Report();
        agent.setName(report.getAgentProcessReport().getProcessAddress().getIdentity().getName());
        agent.setId(report.getAgentProcessReport().getProcessAddress().getIdentity().getUniqueID());
        agent.setNumber(report.getAgentProcessReport().getProcessAddress().getIdentity().getNumber());
        agent.setState(report.getAgentProcessReport().getState().toString());
        for (WorkerProcessReport workerReport : report.getWorkerProcessReports()) {
            agent.addWorkerReport(toWorkerReport(workerReport));
        }
        return agent;
    }

    private Map<String, Short> toWorkerReport(WorkerProcessReport workerReport) {
        Map<String, Short> result = new HashMap<>(2);
        result.put("running-threads", workerReport.getNumberOfRunningThreads());
        result.put("maximum-threads", workerReport.getMaximumNumberOfThreads());
        return result;
    }
}
