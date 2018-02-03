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

package net.grinder.console.webui;

import java.io.*;

/**
 * Reads a file in reverse line order.  A line is read in starting from the tail then is feed back in the original order
 * until the line has been exhausted.  The process is repeated for the next line.
 *
 * Code is taken from an example on source forge:
 * http://stackoverflow.com/questions/8664705/how-to-read-file-from-end-to-start-in-reverse-order-in-java
 */
public class ReverseLineInputStream extends InputStream {

    RandomAccessFile in;

    long currentLineStart = -1;
    long currentLineEnd = -1;
    long currentPos = -1;
    long lastPosInFile = -1;

    public ReverseLineInputStream( String fileName ) throws FileNotFoundException {
        this(new File(fileName));
    }

    public ReverseLineInputStream(File file) throws FileNotFoundException {
        in = new RandomAccessFile(file, "r");
        currentLineStart = file.length();
        currentLineEnd = file.length();
        lastPosInFile = file.length() -1;
        currentPos = currentLineEnd;
    }

    public void findPrevLine() throws IOException {

        currentLineEnd = currentLineStart;

        // There are no more lines, since we are at the beginning of the file and no lines.
        if (currentLineEnd == 0) {
            currentLineEnd = -1;
            currentLineStart = -1;
            currentPos = -1;
            return;
        }

        long filePointer = currentLineStart -1;

        while ( true) {
            filePointer--;

            // we are at start of file so this is the first line in the file.
            if (filePointer < 0) {
                break;
            }

            in.seek(filePointer);
            int readByte = in.readByte();

            // We ignore last LF in file. search back to find the previous LF.
            if (readByte == 0xA && filePointer != lastPosInFile ) {
                break;
            }
        }
        // we want to start at pointer +1 so we are after the LF we found or at 0 the start of the file.
        currentLineStart = filePointer + 1;
        currentPos = currentLineStart;
    }

    public int read() throws IOException {

        if (currentPos < currentLineEnd ) {
            in.seek(currentPos++);
            int readByte = in.readByte();
            return readByte;

        }
        else if (currentPos < 0) {
            return -1;
        }
        else {
            findPrevLine();
            return read();
        }
    }
}