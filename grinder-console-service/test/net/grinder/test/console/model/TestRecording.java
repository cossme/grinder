package net.grinder.test.console.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Recording;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelImplementation;
import net.grinder.console.model.SampleModelViews;
import net.grinder.console.service.Bootstrap;
import net.grinder.statistics.StatisticsServicesImplementation;

/**
 * Created by csolesala on 30/01/2018.
 */
public class TestRecording {

    @Test
    public void testStatus () throws ConsoleException {
        Map<String, String> input = new HashMap<>(3);
        input.put("state", "WaitingForFirstReport");
        input.put("description", "A description");
        input.put("sample-count", "0");
        internalTestStatus(input);

        input = new HashMap<>(3);
        input.put("state", "IgnoringInitialSamples");
        input.put("description", "foo");
        input.put("sample-count", "1");
        internalTestStatus(input);
    }

    private void internalTestStatus(Map<String, String> input) throws ConsoleException {
        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        SampleModel model = Mockito.mock(SampleModel.class);
        ProcessControl pc = Mockito.mock(ProcessControl.class);
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                null,
                model,
                null,
                pc,
                null,
                null ));
        Mockito.when(model.getState()).thenReturn(new SampleModel.State() {
            @Override
            public Value getValue() { return Value.valueOf(input.get("state")); }
            @Override
            public String getDescription() { return input.get("description"); }
            @Override
            public long getSampleCount() { return Long.parseLong(input.get("sample-count")); }
        });
        Recording recording = new Recording();
        ReflectionTestUtils.setField(recording, "model", model);
        Map<String, String> result = recording.status();

        Assert.assertEquals(input, result);
    }

    @Test
    public void testStart() throws ConsoleException {
        Recording recording = new Recording();
        final Wrapper<String> called = new Wrapper<>();
        initializeMock(recording, called);
        recording.start();
        Assert.assertEquals(called.getValue(), "started");
    }

    @Test
    public void testReset () throws ConsoleException {
        Recording recording = new Recording();
        final Wrapper<String> called = new Wrapper<>();
        initializeMock(recording, called);
        recording.reset();
        Assert.assertEquals(called.getValue(), "reset");
    }

    @Test
    public void testZero () throws ConsoleException {
        Recording recording = new Recording();
        final Wrapper<String> called = new Wrapper<>();
        initializeMock(recording, called);
        recording.zero();
        Assert.assertEquals(called.getValue(), "zero");
    }

    @Test
    public void testStop () throws ConsoleException {
        Recording recording = new Recording();
        final Wrapper<String> called = new Wrapper<>();
        initializeMock(recording, called);
        recording.stop();
        Assert.assertEquals(called.getValue(), "stopped");
    }

    private void initializeMock(Recording recording, Wrapper<String> called) throws ConsoleException {
        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        SampleModel model = Mockito.mock(SampleModel.class);
        ReflectionTestUtils.setField(recording, "model", model);
        ProcessControl pc = Mockito.mock(ProcessControl.class);
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                null,
                model,
                null,
                pc,
                null,
                null ));
        Mockito.when(model.getState()).thenReturn(new SampleModel.State() {
            @Override
            public Value getValue() { return Value.Stopped; }
            @Override
            public String getDescription() { return "blah"; }
            @Override
            public long getSampleCount() { return 0; }
        });
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue("stopped");
                return null;
            }
        }).when(model).stop();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue("started");
                return null;
            }
        }).when(model).start();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue("zero");
                return null;
            }
        }).when(model).zeroStatistics();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                called.setValue("reset");
                return null;
            }
        }).when(model).reset();
    }

    @Test
    public void testDataUninitialised () {
        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", null);
        Recording recording = new Recording();
        try {
            recording.data();
            Assert.fail();
        }
        catch (IllegalStateException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testData () throws ConsoleException {
        final Map<String, Object> expectedStatus = new HashMap<>(3);
        expectedStatus.put("sample-count", "99");
        expectedStatus.put("state", "Recording");
        expectedStatus.put("description", "description");

        Recording recording = new Recording();
        SampleModel model = Mockito.mock(SampleModel.class);
        Mockito.when(model.getState()).thenReturn(new SampleModel.State() {
            @Override
            public Value getValue() { return Value.valueOf((String)expectedStatus.get("state")); }
            @Override
            public String getDescription() { return (String)expectedStatus.get("description"); }
            @Override
            public long getSampleCount() { return Long.parseLong((String)expectedStatus.get("sample-count")); }
        });
        Mockito.when(model.getTotalCumulativeStatistics()).thenReturn(
            StatisticsServicesImplementation.getInstance().getStatisticsSetFactory().create());

        SampleModelViews sampleModelView = Mockito.mock(SampleModelViews.class);

        Mockito.when(sampleModelView.getCumulativeStatisticsView()).thenReturn(
                StatisticsServicesImplementation.getInstance().getSummaryStatisticsView());

        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        ProcessControl pc = Mockito.mock(ProcessControl.class);
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                null,
                model,
                sampleModelView,
                pc,
                null,
                null ));
        Map<String, Object>  result = recording.data();
        Map<String, Object> status = (Map)result.get("status");
        List<String> tests = (List)result.get("tests");
        List<String> totals = (List)result.get("totals");
        List<String> columns = (List)result.get("columns");

        Assert.assertEquals(expectedStatus, status);
        Assert.assertEquals(new ArrayList<String>(), tests);
        Assert.assertNotNull(totals);
        Assert.assertEquals(Arrays.asList(
                "Tests,Errors,Mean Test Time (ms),Test Time Standard Deviation (ms),TPS"
                        .split(",")), columns);

    }


    private net.grinder.common.Test makeTest(int id, String label) {
        return new net.grinder.common.Test() {
            @Override
            public int getNumber() { return id; }
            @Override
            public String getDescription() { return label; }
            @Override
            public int compareTo(net.grinder.common.Test o) {
                return new Integer(id).compareTo(new Integer(o.getNumber()));
            }
        };
    }

    @Test
    public void testWithRealSampleModel () throws Exception {
        File f = File.createTempFile("grindertest", "tmp");
        ConsoleProperties consoleProperties = new ConsoleProperties(null, f);

        Resources mockResources = Mockito.mock(Resources.class);
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String)invocation.getArguments()[0];
            }
        }).when(mockResources).getString(Mockito.anyString());

        SampleModel sm = new SampleModelImplementation(
                consoleProperties,
                StatisticsServicesImplementation.getInstance(),
                null,
                mockResources,
                null
        );

        SampleModelViews sampleModelView = Mockito.mock(SampleModelViews.class);
        Mockito.when(sampleModelView.getCumulativeStatisticsView()).thenReturn(
                StatisticsServicesImplementation.getInstance().getSummaryStatisticsView());

        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        ProcessControl pc = Mockito.mock(ProcessControl.class);
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                consoleProperties,
                sm,
                sampleModelView,
                pc,
                null,
                null ));

        Recording recording = new Recording();
        Map<String, String> result = recording.start();
        result = recording.status();
        Assert.assertEquals("WaitingForFirstReport", result.get("state"));
        Assert.assertEquals("state.waiting.label", result.get("description"));


        sm.registerTests(Arrays.asList(new net.grinder.common.Test[] {
                makeTest(1, "test one"),
                makeTest(2, "test two"),
        }));

        Map<String, Object> data = recording.data();

        List<String> columns = (List)data.get("columns");
        List<Number> totals  = (List)data.get("totals");
        List<Map<String,Object>> tests = (List)data.get("tests");

        Assert.assertEquals("[0, 0, NaN, 0.0, NaN]", totals.toString());
        Assert.assertEquals("[Tests, Errors, Mean Test Time (ms), Test Time Standard Deviation (ms), TPS]", columns.toString());
        Assert.assertEquals(2, tests.size());

        Map<String,Object> test = tests.get(0);
        Assert.assertEquals(new Integer(1), (Integer)test.get("test"));
        Assert.assertEquals("test one", (String)test.get("description"));
        Assert.assertEquals("[0, 0, NaN, 0.0, NaN]", test.get("statistics").toString());

        test = tests.get(1);
        Assert.assertEquals(new Integer(2), (Integer)test.get("test"));
        Assert.assertEquals("test two", (String)test.get("description"));
        Assert.assertEquals("[0, 0, NaN, 0.0, NaN]", test.get("statistics").toString());

    }

    @Test
    public void testWithRealSampleModelLatest () throws Exception {
        File f = File.createTempFile("grindertest", "tmp");
        ConsoleProperties consoleProperties = new ConsoleProperties(null, f);

        Resources mockResources = Mockito.mock(Resources.class);
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String)invocation.getArguments()[0];
            }
        }).when(mockResources).getString(Mockito.anyString());

        SampleModel sm = new SampleModelImplementation(
                consoleProperties,
                StatisticsServicesImplementation.getInstance(),
                null,
                mockResources,
                null
        );

        SampleModelViews sampleModelView = Mockito.mock(SampleModelViews.class);
        Mockito.when(sampleModelView.getIntervalStatisticsView()).thenReturn(
                StatisticsServicesImplementation.getInstance().getDetailStatisticsView());

        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        ProcessControl pc = Mockito.mock(ProcessControl.class);
        ReflectionTestUtils.setField(bootstrap, "INSTANCE", new Bootstrap(
                consoleProperties,
                sm,
                sampleModelView,
                pc,
                null,
                null ));

        Recording recording = new Recording();
        Map<String, String> result = recording.start();
        Assert.assertEquals("WaitingForFirstReport", result.get("state"));
        Assert.assertEquals("state.waiting.label", result.get("description"));


        sm.registerTests(Arrays.asList(new net.grinder.common.Test[] {
                makeTest(1, "test one"),
                makeTest(2, "test two"),
        }));

        Map<String, Object> data = recording.dataLatest();

        Map<String, String> status = (Map)data.get("status");
        List<String> columns = (List)data.get("columns");
        List<Number> totals  = (List)data.get("totals");
        List<Map<String,Object>> tests = (List)data.get("tests");

        Assert.assertEquals("[0, 0]", totals.toString());
        Assert.assertEquals("[Test time, Errors]", columns.toString());
        Assert.assertEquals(2, tests.size());

        Map<String,Object> test = tests.get(0);
        Assert.assertEquals(new Integer(1), (Integer)test.get("test"));
        Assert.assertEquals("test one", (String)test.get("description"));
        Assert.assertEquals("[0, 0]", test.get("statistics").toString());

        test = tests.get(1);
        Assert.assertEquals(new Integer(2), (Integer)test.get("test"));
        Assert.assertEquals("test two", (String)test.get("description"));
        Assert.assertEquals("[0, 0]", test.get("statistics").toString());
    }

}
