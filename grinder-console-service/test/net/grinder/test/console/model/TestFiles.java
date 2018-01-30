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

package net.grinder.test.console.model;

import net.grinder.common.GrinderException;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionHandler;
import net.grinder.console.model.DistributionResult;
import net.grinder.console.model.Files;
import net.grinder.console.service.Bootstrap;
import net.grinder.util.FileContents;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Created by solcyr on 29/01/2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestFiles {


    Files files = new Files();

    @Mock
    FileDistribution fileDistribution;

    @Mock
    FileDistributionHandler mockHandler;

    public DistributionResult createMockResult(int percentage, String fileName) {
        DistributionResult result = new DistributionResult();
        result.setPercentComplete(percentage);
        result.addFile(fileName);
        return result;
    }

    @Test
    public void testStatus()  {
        DistributionResult result = createMockResult(30, "a");
        boolean stale = true;
        Mockito.when(fileDistribution.getAgentCacheState()).thenReturn(new AgentCacheState() {
            public boolean getOutOfDate() {  return stale; }
            public void setNewFileTime(long time) {}
            public void addListener(PropertyChangeListener listener) {}
        });
        Whitebox.setInternalState(files, "distributionResult", result);
        Map<String, Object> status = files.status(fileDistribution);
        Assert.assertEquals(status.get("stale"),stale);
        Assert.assertEquals(status.get("last-distribution"),result);
    }


    class NextFileSimulator {
        FileDistributionHandler.Result[] calls;
        int nbCall = 0;
        public NextFileSimulator() {
            this.calls = new FileDistributionHandler.Result[]{
                    createMockFileHanderResult(50, "a"),
                    createMockFileHanderResult(100, "b"),
                    null
            };
            this.nbCall = 0;
        }

        private FileDistributionHandler.Result createMockFileHanderResult(final int percentage, final String fileName) {
            return new  FileDistributionHandler.Result() {
                @Override
                public int getProgressInCents() { return percentage; }
                @Override
                public String getFileName() { return fileName; }
            };
        }

        public  FileDistributionHandler.Result call() {
            return calls[nbCall++];
        }
    }

    class DistributionResultHistory extends DistributionResult {
        public List<DistributionResult> history = new ArrayList<>();
        @Override
        public void setState(String state) {
            super.setState(state);
            history.add(clone());
        }

        public void assertHistoryEntry(int index, int nextId, String state, Integer pctComplete, String[] files, String error) {
            Assert.assertEquals(history.get(index).getId(), nextId);
            Assert.assertEquals(history.get(index).getState(), state);
            if (pctComplete == null) {
                Assert.assertNull(history.get(index).getPercentComplete());
            }
            else {
                Assert.assertEquals(history.get(index).getPercentComplete(), new Integer(pctComplete));
            }
            Assert.assertEquals(history.get(index).getFiles(), Arrays.asList(files));
            if (error == null) {
                Assert.assertNull(history.get(index).getError());
            }
            else {
                Assert.assertEquals(history.get(index).getError(), error);
            }
        }
    }

    @Test
    public void testStartDistribution() throws FileContents.FileContentsException {
        DistributionResultHistory history = new DistributionResultHistory();
        Whitebox.setInternalState(files, "nextId", 22);
        Whitebox.setInternalState(files, "distributionResult", history);

        NextFileSimulator nextFileSimulator = new NextFileSimulator();
        Mockito.when(fileDistribution.getHandler()).thenReturn(mockHandler);
        Mockito.when(mockHandler.sendNextFile()).thenReturn(
                nextFileSimulator.call(),
                nextFileSimulator.call(),
                nextFileSimulator.call()
        );

        DistributionResult result = files.startDistribution(fileDistribution);
        Assert.assertEquals(result.getId(), 23);
        Assert.assertEquals(result.getState(), "started");
        Assert.assertNull(result.getPercentComplete());
        Assert.assertEquals(result.getFiles(), Arrays.asList(new String[] {}));

        while (history.history.size() < 3) {
            try { Thread.sleep(200); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }

        history.assertHistoryEntry(0, 23, "started", null, new String[] {}, null);
        history.assertHistoryEntry(1, 23, "sending", 50, new String[] {"a"}, null);
        history.assertHistoryEntry(2, 23, "sending", 100, new String[] {"a", "b"}, null);
        history.assertHistoryEntry(3, 23, "finished", 100, new String[] {"a", "b"}, null);
    }

    @Test
    public void testStartDistributionBadHandler() throws FileContents.FileContentsException {
        DistributionResultHistory history = new DistributionResultHistory();
        Whitebox.setInternalState(files, "nextId", 22);
        Whitebox.setInternalState(files, "distributionResult", history);

        Mockito.when(fileDistribution.getHandler()).thenReturn(mockHandler);
        Mockito.when(mockHandler.sendNextFile()).thenThrow(new FileContents.FileContentsException("Distribution failed"));

        DistributionResult result = files.startDistribution(fileDistribution);
        Assert.assertEquals(result.getId(), 23);
        Assert.assertEquals(result.getState(), "started");
        Assert.assertNull(result.getPercentComplete());
        Assert.assertEquals(result.getFiles(), Arrays.asList(new String[] {}));

        while (history.history.size() < 2) {
            try { Thread.sleep(200); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }

        history.assertHistoryEntry(0, 23, "started", null, new String[] {}, null);
        history.assertHistoryEntry(1, 23, "error",   null, new String[] {}, "Distribution failed");
    }

}
