package net.grinder.console;

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

import org.springframework.boot.Banner;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.Properties;

/**
 * Created by solcyr on 27/01/2018.
 */

@SpringBootApplication(scanBasePackages={"net.grinder.console"})
public class SpringConsoleFoundation {

    public static void initSpring()  {
        SpringApplication app = new SpringApplication(SpringConsoleFoundation.class);

        app.setBanner(new Banner() {
                          @Override
                          public void printBanner(Environment environment, Class<?> aClass,
                                                  PrintStream printStream) {
                              try {
                                  InputStream in = SpringConsoleFoundation.class.getClassLoader()
                                                   .getResourceAsStream("grinderBanner.txt");
                                  BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                  String line;
                                  while ((line = reader.readLine()) != null) {
                                      printStream.println(line);
                                  }
                                  reader.close();
                              }
                              catch (IOException e) {
                                  e.printStackTrace();
                              }
                          }
                      });
        //app.setBannerMode(Banner.Mode.OFF);
        app.run(new String[]{});
    }

}
