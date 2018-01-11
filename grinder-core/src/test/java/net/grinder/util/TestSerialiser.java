// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2011 Philip Aston
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

package net.grinder.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import junit.framework.TestCase;


/**
 * Unit test case for {@link Serialiser}.
 *
 * @author Philip Aston
 */
public class TestSerialiser extends TestCase
{
    private final Random m_random = new Random();

    public void testUnsignedLongs() throws Exception
    {
	final ByteArrayOutputStream byteArrayOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteArrayOutputStream);

	final long[] longs = new long[10000];

	final Serialiser serialiser = new Serialiser();

	for (int i=0; i<longs.length; i++) {
	    if (i < 1000) {
		longs[i] = i;
	    }
	    else {
		longs[i] = Math.abs(m_random.nextLong() & 0xFFF);
	    }

	    serialiser.writeUnsignedLong(objectOutputStream, longs[i]);
	}

	try {
	    serialiser.writeUnsignedLong(objectOutputStream, -1);
	    fail("Should not reach");
	}
	catch (IOException e) {
	}

	objectOutputStream.close();

	final byte[] bytes = byteArrayOutputStream.toByteArray();

	assertTrue("We should compress", bytes.length < 8 * longs.length);

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(new ByteArrayInputStream(bytes));

	for (int i=0; i<longs.length; i++) {
	    assertEquals(longs[i],
			 serialiser.readUnsignedLong(objectInputStream));
	}
    }

    public void testLongs() throws Exception
    {
	final ByteArrayOutputStream byteArrayOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteArrayOutputStream);

	final long[] longs = new long[3002];

	final Serialiser serialiser = new Serialiser();

	for (int i=0; i<longs.length; i++) {
	    if (i < 1000) {
		longs[i] = i;
	    }
	    else if (i <2000) {
		longs[i] = i - 2000;
	    }
	    else {
		longs[i] = m_random.nextLong();
	    }

	    longs[3000] = Long.MIN_VALUE;
	    longs[3001] = Long.MAX_VALUE;

	    serialiser.writeLong(objectOutputStream, longs[i]);
	}

	objectOutputStream.close();

	final byte[] bytes = byteArrayOutputStream.toByteArray();

	assertTrue("We should compress", bytes.length < 8 * longs.length);

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(new ByteArrayInputStream(bytes));

	for (int i=0; i<longs.length; i++) {
	    assertEquals(longs[i], serialiser.readLong(objectInputStream));
	}
    }

    public void testDoubles() throws Exception
    {
	final ByteArrayOutputStream byteArrayOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteArrayOutputStream);

	final double[] doubles = new double[10000];

	final Serialiser serialiser = new Serialiser();

	for (int i=0; i<doubles.length; i++) {
	    if (i < 1000) {
		doubles[i] = i;
	    }
	    else {
		doubles[i] = m_random.nextDouble();
	    }

	    serialiser.writeDouble(objectOutputStream, doubles[i]);
	}

	objectOutputStream.close();

	final byte[] bytes = byteArrayOutputStream.toByteArray();

	// To do, make this work.
	//assertTrue("We should compress", bytes.length < 8 * doubles.length);

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(new ByteArrayInputStream(bytes));

	for (int i=0; i<doubles.length; i++) {
	    assertEquals(doubles[i], serialiser.readDouble(objectInputStream),
			 0.00001);
	}
    }
}
