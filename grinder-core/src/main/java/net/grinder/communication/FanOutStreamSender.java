// Copyright (C) 2003 - 2012 Philip Aston
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

package net.grinder.communication;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import net.grinder.common.Closer;
import net.grinder.communication.ResourcePool.Resource;
import net.grinder.util.thread.ExecutorFactory;


/**
 * Manages the sending of messages to many streams.
 *
 * @author Philip Aston
 */
public final class FanOutStreamSender extends AbstractFanOutSender {

  /**
   * Constructor.
   *
   * @param numberOfThreads Number of sender threads to use.
   */
  public FanOutStreamSender(int numberOfThreads) {
    this(ExecutorFactory.createThreadPool("FanOutStreamSender",
                                          numberOfThreads));
  }

  /**
   * Constructor.
   *
   * @param executor Executor service to use.
   */
  private FanOutStreamSender(ExecutorService executor) {
    super(executor, new ResourcePoolImplementation());
  }

  /**
   * Add a stream.
   *
   * @param stream The stream.
   */
  public void add(OutputStream stream) {
    getResourcePool().add(new OutputStreamResource(stream));
  }

  /**
   * Shut down this sender.
   */
  @Override public void shutdown() {
    super.shutdown();
    getResourcePool().closeCurrentResources();
  }

  /**
   * Return an output stream from a resource.
   *
   * @param resource The resource.
   * @return The output stream.
   */
  @Override protected OutputStream resourceToOutputStream(
    ResourcePool.Resource resource) {

    return ((OutputStreamResource)resource).getOutputStream();
  }

  /**
   * We don't support addressing individual streams.
   *
   * @param resource The resource.
   * @return The address, or <code>null</code> if the resource has no address.
   */
  @Override protected Address getAddress(Resource resource) {
    return null;
  }

  private static final class OutputStreamResource
          implements ResourcePool.Resource {

    private final OutputStream m_outputStream;

    public OutputStreamResource(OutputStream outputStream) {
      m_outputStream = outputStream;
    }

    public OutputStream getOutputStream() {
      return m_outputStream;
    }

    public void close() {
      Closer.close(m_outputStream);
    }
  }
}
