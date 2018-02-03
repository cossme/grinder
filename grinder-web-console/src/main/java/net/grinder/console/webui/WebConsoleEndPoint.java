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

import net.grinder.Grinder;
import net.grinder.common.GrinderProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by solcyr on 15/12/2017.
 */
@Controller
public class WebConsoleEndPoint {

    private String currentPath;
    private String propertiesFile;
    private GrinderProperties properties;

    WebConsoleEndPoint() {
        File savedDistributionFolder = WebConsoleUI.getInstance().consoleProperties.getDistributionDirectory().getFile();
        if (savedDistributionFolder.exists()) {
            currentPath = savedDistributionFolder.getAbsolutePath().replace('\\', '/');
        }
        else {
            currentPath = System.getProperty("user.dir").replace('\\', '/');
        }
        try {
            properties = new GrinderProperties(WebConsoleUI.getInstance().consoleProperties.getPropertiesFile());
            this.propertiesFile=WebConsoleUI.getInstance().consoleProperties.getPropertiesFile().getAbsolutePath().replace('\\', '/');
        }
        catch (GrinderProperties.PersistenceException e) {
            e.printStackTrace();
            properties = new GrinderProperties();
        }
    }

    @RequestMapping(value="/logs/list", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String logServ(){
        File folder = new File(properties.getProperty("grinder.logDirectory", "log"));
        File[] listOfFiles = folder.listFiles();
        String output = "{\n";
        output += " \"logPath\": \"" + folder.getAbsolutePath().replaceAll("\\\\", "/") + "\",\n" ;
        output += " \"doclog\": { ";
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    output += "\n  \"" + listOfFiles[i].getName() + "\": false,";
                } else if (listOfFiles[i].isDirectory()) {
                    output += "\n  \"" + listOfFiles[i].getName() + "\": true,";
                }
            }
        }
        output = output.substring(0, output.length() - 1) + "\n }\n}\n";
        return output;
    }

    // TODO: Use BASE64 Body
    @RequestMapping("/logs")
    @ResponseBody
    String getLog(@RequestParam(value="doclo", required=true) String logFile){
        String contentFile = "";
        String filePath = logFile;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePath));
            StringWriter out = new StringWriter();
            int b;
            while ((b = in.read()) != -1)
                out.write(b);
            out.flush();
            out.close();
            in.close();
            contentFile = out.toString();
            StringWriter writer = new StringWriter();
            escapeJavaStyleString(writer, contentFile, false);
            contentFile = writer.toString();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        String output = "{\n";
        output += " \"doc\": \"" + contentFile + "\"\n";
        output += "}";
        return output;    }

    // TODO: Use BASE64 Body
    @RequestMapping(value="/filesystem/files", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String getFile(@RequestParam(value="docAdvan", required=true) String filePath){
        String contentFile = "";
        String error = "ok";
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePath));
            StringWriter out = new StringWriter();
            int b;
            while ((b=in.read()) != -1)
                out.write(b);
            out.flush();
            out.close();
            in.close();
            contentFile = out.toString();
            StringWriter writer = new StringWriter();
            escapeJavaStyleString(writer, contentFile, false);
            contentFile = writer.toString();

        }
        catch (IOException ie) {
            ie.printStackTrace();
            error = ie.getMessage();
        }

        String output = "{\n";
        output += " \"doc\": \"" + contentFile + "\",\n";
        output += " \"doc1\": \"" + filePath.replaceAll("\\\\", "/") + "\",\n";
        output += " \"error\": \"" + error +"\"\n";
        output += "}";
        return output;
    }

    // TODO: replace by a put
    // TODO: Use BASE64 Body
    @RequestMapping(value="/filesystem/files/write", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String writeFile(@RequestParam(value="ajaa", required=true) String fileContent,
                     @RequestParam(value="chemup", required=true) String filePath){
        WebConsoleUI.getInstance().logger.info("save Files and write on folder ...");
        String err;
        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.println(fileContent);
            writer.close();
            err="file saved";
        }
        catch (IOException e) {
            WebConsoleUI.getInstance().logger.error("saveas: " + e.getMessage());
            e.printStackTrace();
            err = e.getMessage();
        }
        return "{\"error\": \"" + err + "\"}";
    }

    // TODO: replace by a put
    // TODO: Use BASE64 Body
    @RequestMapping(value="/filesystem/files/save", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String saveAs(@RequestParam(value="ajaa", required=true) String a,
                  @RequestParam(value="newname", required=true) String b){
        WebConsoleUI.getInstance().logger.info("save as ...");
        try {
            PrintWriter writer = new PrintWriter(currentPath + "/" + b, "UTF-8");
            writer.println(a);
            writer.close();
        }
        catch (IOException e) {
            WebConsoleUI.getInstance().logger.error("saveas: " + e.getMessage());
            e.printStackTrace();
        }
        return "{\"docss\":1}";

    }

    @RequestMapping(value="/filesystem/files/list", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String listFiles(){
        File folder = new File(currentPath);
        File[] listOfFiles = folder.listFiles();
        String output = "{\n";
        output += " \"chemfile\": \"" + currentPath + "\",\n";
        output += " \"docss2\": { ";

        if (listOfFiles != null && listOfFiles.length > 0) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    output += "\n  \"" + listOfFiles[i].getName() + "\": false,";
                } else if (listOfFiles[i].isDirectory()) {
                    output += "\n  \"" + listOfFiles[i].getName() + "\": true,";
                }
            }
            output = output.substring(0, output.length() - 1);
        }
        output += "\n }\n}\n";
        return output;
    }

    @RequestMapping(value="/filesystem/directory/change", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String changeChem2(@RequestParam(value="newPath2", required=true) String newPath){
        currentPath = newPath.replace('\\', '/');
        return "{\"Pathes2\": \"" + currentPath + "\"}";
    }


    @RequestMapping(value="/logs/delete", method = RequestMethod.DELETE, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String deleteLogs(){
        String err = "ok";
        try {
            WebConsoleUI.getInstance().logger.info("deleting logs");
            File[] listOfFiles = new File(properties.getProperty("grinder.logDirectory", "log")).listFiles();
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    listOfFiles[i].delete();
                }
            }
        }

        catch (Exception e) {
            e.printStackTrace();
            WebConsoleUI.getInstance().logger.error("deleting logs failed - " + e.getMessage());
            err = e.getMessage();
        }
        return "{\"error\": \"" + err + "\"}";
    }

    @RequestMapping(value="/filesystem/file/download", method = RequestMethod.GET)
    ResponseEntity<Resource> downloadFile(@RequestParam(value="logFile", required=false) String logFile) throws IOException{
        File file = new File(logFile);
        Path path = Paths.get(file.getAbsolutePath());
        Resource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                //.headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }

    @RequestMapping(value="/agents/start", method = RequestMethod.POST, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String startNewAgent() {
        Grinder.main(new String[] {});
        return "success";
    }

    private void escapeJavaStyleString(Writer out, String str, boolean escapeSingleQuote) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        }
        if (str == null) {
            return;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                out.write("\\u" + Integer.toHexString(ch));
            } else if (ch > 0xff) {
                out.write("\\u0" + Integer.toHexString(ch));
            } else if (ch > 0x7f) {
                out.write("\\u00" + Integer.toHexString(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        out.write('\\');
                        out.write('b');
                        break;
                    case '\n':
                        out.write('\\');
                        out.write('n');
                        break;
                    case '\t':
                        out.write('\\');
                        out.write('t');
                        break;
                    case '\f':
                        out.write('\\');
                        out.write('f');
                        break;
                    case '\r':
                        out.write('\\');
                        out.write('r');
                        break;
                    default :
                        if (ch > 0xf) {
                            out.write("\\u00" + Integer.toHexString(ch));
                        } else {
                            out.write("\\u000" + Integer.toHexString(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':
                        if (escapeSingleQuote) {
                            out.write('\\');
                        }
                        out.write('\'');
                        break;
                    case '"':
                        out.write('\\');
                        out.write('"');
                        break;
                    case '\\':
                        out.write('\\');
                        out.write('\\');
                        break;
                    default :
                        out.write(ch);
                        break;
                }
            }
        }
    }
}