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

package net.grinder.test.console.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import net.grinder.common.GrinderBuild;
import net.grinder.console.SpringConsoleFoundation;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.ErrorQueue;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.common.StubResources;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.DistributionControl;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControlImplementation;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionImplementation;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Files;
import net.grinder.console.model.Processes;
import net.grinder.console.model.Properties;
import net.grinder.console.model.Recording;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.console.service.Bootstrap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.SocketUtilities;
import net.grinder.testutility.StubTimer;
import net.grinder.util.Directory;
import net.grinder.util.StandardTimeAuthority;
import net.grinder.util.TimeAuthority;

/**
 * Created by solcyr on 28/01/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes={SpringConsoleFoundation.class})
@AutoConfigureMockMvc
public class RestControllerTest  extends AbstractJUnit4FileTestCase {

    public Processes processes = new Processes();

    public Files files     = new Files();

    public Recording recording = new Recording();

    private @Mock ErrorHandler m_errorHandler;

    private FileDistribution fileDistribution;

    private ErrorQueue errorQueue = new ErrorQueue();

    private final TimeAuthority m_timeAuthority = new StandardTimeAuthority();

    private StubTimer m_timer = new StubTimer();

    private ConsoleProperties m_properties;

    private static final Resources s_resources =
            new ResourcesImplementation(
                    "net.grinder.console.common.resources.Console");

    private final Resources m_resources = new StubResources<String>(
            new HashMap<String, String>() {{
                put("finished.text", "done");
                put("noConnectedAgents.text", "no agents!");
                put("processTable.threads.label", "strings");
                put("processTable.agentProcess.label", "AG");
                put("processTable.workerProcess.label", "WK");
                put("processState.started.label", "hot to trot");
                put("processState.running.label", "rolling");
                put("processState.connected.label", "plugged in");
                put("processState.finished.label", "fini");
                put("processState.disconnected.label", "that's all folks");
                put("processState.unknown.label", "huh");
            }}
    );

    private ConsoleCommunicationImplementation m_consoleCommunication;

    private final RandomStubFactory<SampleModel> m_sampleModelStubFactory =
            RandomStubFactory.create(SampleModel.class);

    private final SampleModel modelParam = m_sampleModelStubFactory.getStub();

    private final RandomStubFactory<SampleModelViews> m_sampleModelViewsStubFactory =
            RandomStubFactory.create(SampleModelViews.class);

    @Before
    public void setUp() throws IOException, ConsoleException{

        final File file = new File(getDirectory(), "properties");

        m_properties = new ConsoleProperties(s_resources, file);

        m_properties.setConsolePort(SocketUtilities.findFreePort());

        m_consoleCommunication =
                new ConsoleCommunicationImplementation(s_resources,
                        m_properties,
                        m_errorHandler,
                        m_timeAuthority,
                        10,
                        10000);

        final ProcessControl processControl =
                new ProcessControlImplementation(m_timer,
                        m_consoleCommunication,
                        s_resources);

        final RandomStubFactory<DistributionControl> distributionControlStubFactory =
                RandomStubFactory.create(DistributionControl.class);

        final DistributionControl distributionControl = distributionControlStubFactory.getStub();

        fileDistribution =
                new FileDistributionImplementation(distributionControl,
                        processControl,
                        new Directory(getDirectory()),
                        Pattern.compile("^.grinder/$"));

        StatisticsServices statisticsServices = StatisticsServicesTestFactory.createTestInstance();

        m_sampleModelViewsStubFactory.setResult("getIntervalStatisticsView",
                statisticsServices.getSummaryStatisticsView());
        m_sampleModelViewsStubFactory.setResult("getCumulativeStatisticsView",
                statisticsServices.getSummaryStatisticsView());


        m_sampleModelViewsStubFactory.setResult("getTotalLatestStatistics",
                statisticsServices.getStatisticsSetFactory().create());
        m_sampleModelStubFactory.setResult("getTotalCumulativeStatistics",
                statisticsServices.getStatisticsSetFactory().create());

        m_sampleModelViewsStubFactory.setResult("getTestStatisticsQueries",
                statisticsServices.getTestStatisticsQueries());

        m_sampleModelViewsStubFactory.setResult("getNumberFormat",
                new DecimalFormat("0.0"));

        SampleModelViews sampleModelViewsParam =
                m_sampleModelViewsStubFactory.getStub();

        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);

        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                m_properties,
                modelParam,
                sampleModelViewsParam,
                processControl,
                errorQueue,
                fileDistribution ));

        processes.init();
        recording.init();
    }

    @Autowired
    private MockMvc restController;

    @Test
    public void testVersion() throws Exception {
        this.restController.perform(MockMvcRequestBuilders.get("/version")).andExpect(
                MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(
                        Matchers.containsString(GrinderBuild.getName())));

    }

    private void assertGetJson(String uri, HttpStatus status, Object responseBody) throws Exception {
        this.restController.perform(MockMvcRequestBuilders.get(uri)).andExpect(
                MockMvcResultMatchers.status().is(status.value()))
                .andExpect(MockMvcResultMatchers.content().json(
                        new ObjectMapper().writer().writeValueAsString(responseBody)));
    }

    private void assertPostJson(String uri, HttpStatus status, Object responseBody) throws Exception {
        this.restController.perform(MockMvcRequestBuilders.post(uri)).andExpect(
                MockMvcResultMatchers.status().is(status.value()))
                .andExpect(MockMvcResultMatchers.content().json(
                        new ObjectMapper().writer().writeValueAsString(responseBody)));
    }

    private void assertPostString(String uri, HttpStatus status, String responseBody) throws Exception {
        this.restController.perform(MockMvcRequestBuilders.post(uri)).andExpect(
                MockMvcResultMatchers.status().is(status.value()))
                .andExpect(MockMvcResultMatchers.content().string(responseBody));
    }

    @Test
    public void testBasicRoutes() throws Exception{
        assertGetJson( "/agents/status", HttpStatus.OK, processes.status());
        assertPostString( "/agents/stop", HttpStatus.OK, processes.stopAgents());
        assertPostString( "/agents/stop-workers", HttpStatus.OK, processes.stopWorkers());
        assertPostJson( "/files/distribute", HttpStatus.OK, files.startDistribution(fileDistribution));
        assertGetJson( "/files/status", HttpStatus.OK, files.status(fileDistribution));
        assertGetJson( "/properties", HttpStatus.OK, Properties.getProperties(Bootstrap.getConsoleProperties()));
        assertPostString( "/properties/save", HttpStatus.OK, "success");
        assertPostJson( "/recording/start", HttpStatus.OK, recording.start());
        assertGetJson( "/recording/status", HttpStatus.OK, recording.status());
        assertGetJson( "/recording/data", HttpStatus.OK, recording.data());
        assertGetJson( "/recording/data-latest", HttpStatus.OK, recording.dataLatest());
        assertPostJson( "/recording/stop", HttpStatus.OK, recording.stop());
        assertPostJson( "/recording/zero", HttpStatus.OK, recording.zero());
        assertPostJson( "/recording/reset", HttpStatus.OK, recording.reset());
    }

    @Test
    public void testUnknownRoutes() throws Exception{
        try {
          this.restController.perform(MockMvcRequestBuilders.put("agents/status"));
          AssertUtilities.fail ("IllegalArgumentException Expected") ;
        }
        catch (IllegalArgumentException e) {
          AssertUtilities.assertContains(e.getMessage(), "'url' should start with a path");
        }
        this.restController.perform(MockMvcRequestBuilders.put("/agents/status"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
        this.restController.perform(MockMvcRequestBuilders.put("/agents/stop"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
        this.restController.perform(MockMvcRequestBuilders.put("/recording/zero"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());

    }

    @Test
    public void testPutProperties() throws Exception{
        Map<String, String> newProperties = new HashMap<>(2);
        newProperties.put("distributionDirectory", "examples");
        newProperties.put("propertiesFile", "grinder.properties");
        this.restController.perform(MockMvcRequestBuilders.put(
                "/properties").content(
                new ObjectMapper().writer().writeValueAsString(newProperties))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        new ObjectMapper().writer().writeValueAsString(newProperties)));
    }

    @Test
    public void testStartWorkers() throws Exception{
        Map<String, String> startProperties = new HashMap<>(3);
        startProperties.put("grinder.processes", "2");
        startProperties.put("grinder.threads", "5");
        startProperties.put("grinder.runs", "3");
        this.restController.perform(MockMvcRequestBuilders.post(
                "/agents/start-workers").content(
                new ObjectMapper().writer().writeValueAsString(startProperties))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("success"));
    }

}
