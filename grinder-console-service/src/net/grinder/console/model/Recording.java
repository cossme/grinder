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

import net.grinder.common.Test;
import net.grinder.console.service.Bootstrap;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsView;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by solcyr on 28/01/2018.
 */
public class Recording {

    SampleModel model = null;
    SampleModelViews statisticsView = null;
    ModelTestIndex latestTestIndex = null;
    Set<Test> latestTests = null;

    public void init() {
        if (this.model == null) {
            if (Bootstrap.getInstance() != null) {
                this.model = Bootstrap.getInstance().getModel();
                this.statisticsView = Bootstrap.getInstance().getSampleModelViews();
                latestTestIndex = new ModelTestIndex();
                latestTests = new HashSet<>();

                this.model.addModelListener(new SampleModel.Listener() {
                    @Override
                    public void stateChanged() {
                    }

                    @Override
                    public void newSample() {
                    }

                    @Override
                    public void newTests(Set<Test> newTests, ModelTestIndex modelTestIndex) {
                        latestTestIndex = modelTestIndex;
                        latestTests = newTests;
                    }

                    @Override
                    public void resetTests() {
                        this.newTests(null, new ModelTestIndex());
                    }
                });
            }
            else {
                throw new IllegalStateException("Recording not correctly initialized.");
            }
        }
    }

    /**
     * Start the sample model. Returns the current status, as per 'status'.
     */
    public Map<String, String> stop() {
        init();
        this.model.stop();
        return status();
    }

    /**
     * Start the sample model. Returns the current status, as per 'status'.
     *
     * @return
     */
    public Map<String, String> start() {
        init();
        this.model.start();
        return status();
    }

    /**
     * Zero the sample model statistics. Returns the current status, as per
     * 'status'..
     *
     * @return
     */
    public Map<String, String> zero() {
        init();
        this.model.zeroStatistics();
        return status();
    }

    /**
     * After a reset, the model loses all knowledge of Tests; this can be
     * useful when swapping between scripts. It makes sense to reset with
     * the worker processes stopped.  Returns the current status, as per
     * 'status'.
     *
     * @return
     */
    public Map<String, String> reset() {
        init();
        this.model.reset();
        return status();
    }

    /**
     * Return a map summarising the state of the provided SampleModel.
     * The map has the following keys:
     * :state        One of { :Stopped, :WaitingForFirstReport,
     * :IgnoringInitialSamples, :Recording }.
     * :description  A text description of the state.
     * :sample-count The sample count. Only present if :state is
     * :IgnoringInitialSamples or :Recording.
     */
    public Map<String, String> status() {
        init();
        SampleModel.State s = model.getState();
        String description = "";
        String state = "unknown";
        long sampleCount = 0;
        if (s.getValue() != null) {
            description = s.getDescription();
            state = s.getValue().toString();
            sampleCount = s.getSampleCount();
        }
        Map<String, String> result = new HashMap<>();
        result.put("state", state);
        result.put("description", description);
        result.put("sample-count", Long.toString(sampleCount));
        return result;
    }

    /**
     * Return a map containing the current recording data.
     * The map has the following keys:
     * :status The sample model status as a map, see 'status'.
     * :columns Vector of column names, in same order as statistics vectors.
     * :tests Vector of test data maps, one per test.
     * :totals Vector of total statistics.
     * Each test data map has the following keys:
     * :test The test number.
     * :description The test description.
     * :statistics Vector of statistics.
     *
     * @return
     */
    public Map<String, Object> data() {
        init();
        return getData(
                statisticsView.getCumulativeStatisticsView(),
                model.getTotalCumulativeStatistics(),
                "getCumulativeStatistics");
    }

    /**
     * Get the latest sample data.
     * The result has the same structure as that of (data).
     */
    public Map<String, Object> dataLatest() {
        init();
        return getData(
                statisticsView.getIntervalStatisticsView(),
                model.getTotalLatestStatistics(),
                "getLastSampleStatistics");
    }

    /**
     * @return
     */
    public Map<String, Object> getData(StatisticsView view, StatisticsSet total, String typeOfStat) {
        ExpressionView[] views = view.getExpressionViews();
        List<String> columns = new ArrayList<>(views.length);
        for (ExpressionView v: views) {
            columns.add(v.getDisplayName());
        }
        List<Map<String,Object>> tests = new ArrayList<>(latestTestIndex.getNumberOfTests());
        for (int i = 0; i < latestTestIndex.getNumberOfTests(); ++i) {
            Test test = latestTestIndex.getTest(i);
            Map<String,Object> testInfo = new HashMap<>();
            testInfo.put("test", test.getNumber());
            testInfo.put("description", test.getDescription());
            try {
                Method statGetter = latestTestIndex.getClass().getMethod(typeOfStat, int.class);
                testInfo.put("statistics", processStatistics(views,
                        (StatisticsSet)statGetter.invoke(latestTestIndex, i)));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            tests.add(testInfo);
        }
        Map<String,Object> result = new HashMap<>();
        result.put("status", status());
        result.put("columns", columns);
        result.put("tests", tests);
        result.put("totals", processStatistics(views, total));
        return result;

    }

    private List <Number> processStatistics(ExpressionView[] views, StatisticsSet statistics) {
        if (statistics == null) {
            return new ArrayList<>(0);
        }
        List <Number> result = new ArrayList<>(views.length);
        for (int i =0; i < views.length; ++i) {
            StatisticExpression e = views[i].getExpression();
            try {
                if (e.isDouble()) {
                    result.add(e.getDoubleValue(statistics));
                }
                else {
                        result.add(e.getLongValue(statistics));
                }
            }
            catch (NullPointerException npe) {
                result.add(0);
            }
        }
        return result;
    }
}
