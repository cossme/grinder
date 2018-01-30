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

import net.grinder.common.GrinderException;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionHandler;
import net.grinder.util.FileContents;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by solcyr on 28/01/2018.
 */
public class Files {

    /**
     * The ID to use for the next distribution request.
     */
    private int nextId = 0;

    /**
     * The result of the last distribution.
     */
    public DistributionResult distributionResult = new DistributionResult();

    /**
     * Initiate a new file distribution. Distributions are executed serially;
     * new distribution requests will be queued and processed in order.
     * Returns a map with the sane format as the :last-distribution map of
     * (status).
     */
    public DistributionResult startDistribution(FileDistribution fd) {
        distributionResult.clear();
        distributionResult.setId   (++nextId);
        distributionResult.setState("started");
        new Thread() {
            public void run () {
                fd.scanDistributionFiles();
                final FileDistributionHandler distributionHandler = fd.getHandler();
                try {
                    FileDistributionHandler.Result result = distributionHandler.sendNextFile();
                    while (result != null) {
                        distributionResult.setPercentComplete(result.getProgressInCents());
                        distributionResult.addFile(result.getFileName());
                        distributionResult.setState("sending");
                        result = distributionHandler.sendNextFile();
                    }
                }
                catch (GrinderException e) {
                    distributionResult.setError(e.getMessage());
                    distributionResult.setState("error");
                }
                distributionResult.setState("finished");
            }
        }.start();
        return distributionResult;
    }

    /**
     * Return a map containing the current state of the file distribution.
     * The map has the following keys:
     * :stale A boolean value, true if any of the agent caches are
     * known to be out of date.
     * :last-distribution The result of the last file distribution.
     * The distribution result map has the following keys:
     * :id The identity of the distribution request.
     * :state One of [:started :sending :finished :error].
     * :files The files that have been sent.
     * :exception If the state is :error, an exception.
     */
     public Map<String, Object> status(FileDistribution fd) {
         Map<String, Object> result = new HashMap<>();

         AgentCacheState cacheState = fd.getAgentCacheState();
         boolean stale = cacheState.getOutOfDate();
         result.put("stale", stale);
         result.put("last-distribution", distributionResult);
         return result;
     }
}
