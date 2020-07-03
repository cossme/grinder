package net.grinder.test.console.model;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.*;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Processes;
import net.grinder.console.model.Report;
import net.grinder.console.model.SampleModel;
import net.grinder.console.service.Bootstrap;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.console.AgentAndCacheReport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by csolesala on 29/01/2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestProcesses {

    @Mock
    ProcessControl pc;

    @Mock
    SampleModel model;

    @Mock
    Bootstrap bootstrap;



    @Test
    public void testAgentsStop() {
        final Wrapper<Boolean> called = new Wrapper(false);
        Processes processes = new Processes();
        ReflectionTestUtils.setField(processes, "pc", pc);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue(!called.getValue());
                return null;
            }
        }).when(pc).stopAgentAndWorkerProcesses();

        String result = processes.stopAgents();
        Assert.assertEquals(result, "success");
        Assert.assertTrue(called.getValue());
    }

    @Test
    public void testWorkersStartEmptyProperties()throws Exception {
        wokersStart(new HashMap<String, String>(), new HashMap<String, String>());
    }

    @Test
    public void testWorkersStarWithProperties()throws Exception {
        Map<String, String> userProperties = new HashMap();
        userProperties.put("f1", "v1");
        userProperties.put("f2", "v2");
        userProperties.put("grinder.runs", "99");

        Map<String, String> expectedProperties = new HashMap();
        expectedProperties.put("f1", "v1");
        expectedProperties.put("f2", "v2");
        expectedProperties.put("grinder.runs", "99");

        wokersStart(userProperties, expectedProperties);
    }

    private void wokersStart(Map<String, String> userProperties,
                             Map<String, String> expectedProperties)
                                       throws Exception {
        final Wrapper<GrinderProperties> called = new Wrapper();

        Processes processes = new Processes();
        ReflectionTestUtils.setField(processes, "pc", pc);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue((GrinderProperties)invocation.getArguments()[0]);
                return null;
            }
        }).when(pc).startWorkerProcesses(Mockito.anyObject());


        File f = File.createTempFile("grindertest", "tmp");
        final ConsoleProperties consoleProperties = new ConsoleProperties(null, f);
        String result = processes.startWorkers(consoleProperties, userProperties);
        Assert.assertEquals(result, "success");
        Assert.assertEquals(called.getValue(), expectedProperties);
    }



    @Test
    public void testWorkersStartDistributedEmptyProperties()throws Exception {
        wokersStartDistributed(new Properties(), new HashMap<String, String>(), new HashMap<String, String>());
    }

    @Test
    public void testWorkersStartDistributedWithProperties()throws Exception {
        Properties selectedProperties = new Properties();
        selectedProperties.put("grinder.runs", "1");
        selectedProperties.put("grinder.threads", "2");

        Map<String, String> userProperties = new HashMap();
        userProperties.put("f1", "v1");
        userProperties.put("f2", "v2");
        userProperties.put("grinder.runs", "99");

        Map<String, String> expectedProperties = new HashMap();
        expectedProperties.put("f1", "v1");
        expectedProperties.put("f2", "v2");
        expectedProperties.put("grinder.threads", "2");
        expectedProperties.put("grinder.runs", "99");

        wokersStartDistributed(selectedProperties, userProperties, expectedProperties);
    }

    private void wokersStartDistributed(Properties selectedProperties,
                             Map<String, String> userProperties,
                             Map<String, String> expectedProperties)
            throws Exception {
        final Wrapper<GrinderProperties> called = new Wrapper();

        Processes processes = new Processes();
        ReflectionTestUtils.setField(processes, "pc", pc);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue((GrinderProperties)invocation.getArguments()[1]);
                return null;
            }
        }).when(pc).startWorkerProcessesWithDistributedFiles(Mockito.anyObject(), Mockito.anyObject());


        File f = File.createTempFile("grindertest", "tmp");
        final ConsoleProperties consoleProperties = new ConsoleProperties(null, f);
        FileWriter fileWriter = new FileWriter(f);
        selectedProperties.store(fileWriter, "");
        fileWriter.close();
        consoleProperties.setPropertiesFile(f);
        String result = processes.startWorkers(consoleProperties, userProperties);
        Assert.assertEquals(result, "success");
        Assert.assertEquals(called.getValue(), expectedProperties);
    }

    @Test
    public void testWorkersStop() {
        final Wrapper<Boolean> called = new Wrapper(false);
        Processes processes = new Processes();
        ReflectionTestUtils.setField(processes, "pc", pc);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue(!called.getValue());
                return null;
            }
        }).when(pc).resetWorkerProcesses();

        String result = processes.stopWorkers();
        Assert.assertEquals(result, "success");
        Assert.assertTrue(called.getValue());
    }

    @Test
    public void testStatusWithNoReports() throws ConsoleException {
        final Wrapper<ProcessControl.Listener> listener = new Wrapper();

        Processes processes = new Processes();
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                null,
                model,
                null,
                pc,
                null,
                null ));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                listener.setValue((ProcessControl.Listener) invocation.getArguments()[0]);
                return null;
            }
        }).when(pc).addProcessStatusListener(Mockito.anyObject());

        processes.init();

        listener.getValue().update(new ProcessControl.ProcessReports[]{});
        Assert.assertEquals(processes.status(), new ArrayList<ProcessControl.ProcessReports>());
    }

    @Test
    public void testStatusWithReports() throws ConsoleException {
        final Wrapper<ProcessControl.Listener> listener = new Wrapper();

        Processes processes = new Processes();
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                null,
                model,
                null,
                pc,
                null,
                null ));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                listener.setValue((ProcessControl.Listener) invocation.getArguments()[0]);
                return null;
            }
        }).when(pc).addProcessStatusListener(Mockito.anyObject());

        processes.init();

        ProcessControl.ProcessReports[] reports = new ProcessControl.ProcessReports[1];
        AgentIdentity agentIdentity = new AgentIdentity() {
            @Override
            public int getNumber() {  return 10; }
            @Override
            public String getName() { return "foo"; }
            @Override
            public String getUniqueID() { return "1"; }
        };
        reports[0] = new ProcessControl.ProcessReports() {
            @Override
            public AgentAndCacheReport getAgentProcessReport() {
                return new AgentAndCacheReport() {
                    @Override
                    public ProcessAddress<? extends ProcessIdentity> getProcessAddress() {
                        return new ProcessAddress<ProcessIdentity>(new ProcessIdentity() {
                            @Override
                            public String getName() {
                                return "bar";
                            }

                            @Override
                            public int getNumber() {
                                return 51;
                            }

                            @Override
                            public String getUniqueID() {
                                return "16";
                            }
                        }){};
                    }
                    @Override
                    public State getState() { return State.RUNNING; }
                    @Override
                    public CacheHighWaterMark getCacheHighWaterMark() {  return null; }
                    @Override
                    public AgentIdentity getAgentIdentity() {
                        return agentIdentity;
                    }
                };
            }
            @Override
            public WorkerProcessReport[] getWorkerProcessReports() {
                WorkerProcessReport[] result = new WorkerProcessReport[1];
                result[0] = new WorkerProcessReport() {
                    @Override
                    public WorkerIdentity getWorkerIdentity() {
                        return new WorkerIdentity() {
                            @Override
                            public AgentIdentity getAgentIdentity() { return agentIdentity; }
                            @Override
                            public int getNumber() { return 13; }
                            @Override
                            public String getName() { return "bah"; }
                            @Override
                            public String getUniqueID() { return "9"; }
                        };
                    }

                    @Override
                    public short getNumberOfRunningThreads() {
                        return 2;
                    }

                    @Override
                    public short getMaximumNumberOfThreads() {
                        return 22;
                    }

                    @Override
                    public ProcessAddress<? extends ProcessIdentity> getProcessAddress() {
                        return null;
                    }

                    @Override
                    public State getState() {
                        return State.STARTED;
                    }
                };
                return result;
            }
        };

        listener.getValue().update(reports);

        Report expectedReport = new Report();
        expectedReport.setState("RUNNING");
        expectedReport.setName("bar");
        expectedReport.setId("16");
        expectedReport.setNumber(51);
        Map<String, Short> workerReport = new HashMap<>(2);
        workerReport.put("running-threads", (short)2);
        workerReport.put("maximum-threads", (short)22);
        expectedReport.addWorkerReport(workerReport);
        Assert.assertEquals(expectedReport, processes.status().get(0));
    }


    @Test
    public void testStatusUninitialised() throws ConsoleException {
        Processes processes = new Processes();
        List<Report> result = processes.status();
        Assert.assertTrue(result.isEmpty());
    }


    @Test
    public void testStatusInitialisedDifferentPC() throws ConsoleException {

        final Wrapper<ProcessControl.Listener> listener = new Wrapper();

        Processes processes = new Processes();
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                null,
                model,
                null,
                pc,
                null,
                null ));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                listener.setValue((ProcessControl.Listener) invocation.getArguments()[0]);
                return null;
            }
        }).when(pc).addProcessStatusListener(Mockito.anyObject());

        processes.init();

        ProcessControl.ProcessReports[] reports = new ProcessControl.ProcessReports[1];
        AgentIdentity agentIdentity = new AgentIdentity() {
            @Override
            public int getNumber() {  return 10; }
            @Override
            public String getName() { return "foo"; }
            @Override
            public String getUniqueID() { return "1"; }
        };
        reports[0] = new ProcessControl.ProcessReports() {
            @Override
            public AgentAndCacheReport getAgentProcessReport() {
                return new AgentAndCacheReport() {
                    @Override
                    public ProcessAddress<? extends ProcessIdentity> getProcessAddress() {
                        return new ProcessAddress<ProcessIdentity>(new ProcessIdentity() {
                            @Override
                            public String getName() {
                                return "bar";
                            }

                            @Override
                            public int getNumber() {
                                return 51;
                            }

                            @Override
                            public String getUniqueID() {
                                return "16";
                            }
                        }){};
                    }
                    @Override
                    public State getState() { return State.RUNNING; }
                    @Override
                    public CacheHighWaterMark getCacheHighWaterMark() {  return null; }
                    @Override
                    public AgentIdentity getAgentIdentity() {
                        return agentIdentity;
                    }
                };
            }
            @Override
            public WorkerProcessReport[] getWorkerProcessReports() {
                WorkerProcessReport[] result = new WorkerProcessReport[1];
                result[0] = new WorkerProcessReport() {
                    @Override
                    public WorkerIdentity getWorkerIdentity() {
                        return new WorkerIdentity() {
                            @Override
                            public AgentIdentity getAgentIdentity() { return agentIdentity; }
                            @Override
                            public int getNumber() { return 13; }
                            @Override
                            public String getName() { return "bah"; }
                            @Override
                            public String getUniqueID() { return "9"; }
                        };
                    }

                    @Override
                    public short getNumberOfRunningThreads() {
                        return 2;
                    }

                    @Override
                    public short getMaximumNumberOfThreads() {
                        return 22;
                    }

                    @Override
                    public ProcessAddress<? extends ProcessIdentity> getProcessAddress() {
                        return null;
                    }

                    @Override
                    public State getState() {
                        return State.STARTED;
                    }
                };
                return result;
            }
        };

        listener.getValue().update(reports);

        Processes processes2 = new Processes();
        List<Report> result = processes2.status();
        Assert.assertTrue(result.isEmpty());
    }
}
