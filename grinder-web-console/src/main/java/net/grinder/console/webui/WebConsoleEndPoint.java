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
import net.grinder.console.ConsoleFoundation;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.util.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by solcyr on 15/12/2017.
 */
@Controller
public class WebConsoleEndPoint {

    final Resources resources = new ResourcesImplementation(
            "net.grinder.console.common.resources.Console");
    private String currentPath;
    private GrinderProperties properties;
    final Logger logger = LoggerFactory.getLogger(resources.getString("shortTitle"));

    WebConsoleEndPoint() {
        File savedDistributionFolder = ConsoleFoundation.PROPERTIES.getDistributionDirectory().getFile();
        if (savedDistributionFolder.exists()) {
            currentPath = savedDistributionFolder.getAbsolutePath().replace('\\', '/');
        }
        else {
            currentPath = System.getProperty("user.dir").replace('\\', '/');
        }
        try {
            if (ConsoleFoundation.PROPERTIES.getPropertiesFile() != null) {
                properties = new GrinderProperties(ConsoleFoundation.PROPERTIES.getPropertiesFile());
            }
            else {
                properties = new GrinderProperties();
            }
        }
        catch (GrinderProperties.PersistenceException e) {
            e.printStackTrace();
            properties = new GrinderProperties();
        }
        if (Boolean.getBoolean("grinder.startEmbeddedAgent")) {
            this.startNewAgent();
        }
    }

    @RequestMapping(value="/logs/list", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, Object> logServ(){
        File folder = new File(properties.getProperty("grinder.logDirectory", "log"));
        Map<String, Object> result = new HashMap<>();
        result.put("logPath", folder.getAbsolutePath().replaceAll("\\\\", "/"));

        File[] listOfFiles = folder.listFiles();
        Map<String, Boolean> logFiles = new HashMap<>();
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                logFiles.put(listOfFiles[i].getName(), listOfFiles[i].isDirectory());
            }
        }
        result.put("logFiles", logFiles);
        return result;
    }

    @RequestMapping(value="/logs", produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String getLog(@RequestParam(value="logFile", required=true) String logFile){
        String fileContent = "";
        String filePath = logFile;
        try {
            ReverseLineInputStream ris = new ReverseLineInputStream(filePath);
            InputStreamReader isr = new InputStreamReader (ris);
            BufferedReader in = new BufferedReader(isr);
            // We read the last 5000 lines of the file (to avoid loading to much data in memory)
            for (int i = 0; i < 5000; ++i) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                fileContent = line + "\n" + fileContent;
            }

            in.close();
            isr.close();
            ris.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return DatatypeConverter.printBase64Binary(fileContent.getBytes());
    }


    @RequestMapping(value="/logs/delete", method = RequestMethod.DELETE, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String deleteLogs(){
        String result = "success";
        try {
            logger.info("deleting logs");
            File[] listOfFiles = new File(properties.getProperty("grinder.logDirectory", "log")).listFiles();
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    listOfFiles[i].delete();
                }
            }
        }

        catch (Exception e) {
            e.printStackTrace();
            logger.error("deleting logs failed - " + e.getMessage());
            result = e.getMessage();
        }
        return result;
    }

    // TODO: Use BASE64 Body
    @RequestMapping(value="/filesystem/files", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, Object> getFile(@RequestParam(value="file", required=true) String filePath){
        String contentFile = "";
        String error = "success";
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
            contentFile = DatatypeConverter.printBase64Binary(contentFile.getBytes());

        }
        catch (IOException ie) {
            ie.printStackTrace();
            error = ie.getMessage();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("doc", contentFile);
        result.put("filePath", filePath.replaceAll("\\\\", "/"));
        result.put("error", error);
        return result;
    }

    @RequestMapping(value="/filesystem/files/write", method = RequestMethod.PUT, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String writeFile(@RequestBody Map<String, String> body){
        logger.info("save Files and write on folder ...");
        String err;
        try {
            PrintWriter writer = new PrintWriter(body.get("filePath"), "UTF-8");
            writer.print(new String(DatatypeConverter.parseBase64Binary(body.get("fileContent"))));
            writer.close();
            err="success";
        }
        catch (IOException e) {
            logger.error("saveas: " + e.getMessage());
            e.printStackTrace();
            err = e.getMessage();
        }
        return err;
    }

    @RequestMapping(value="/filesystem/files/save", method = RequestMethod.PUT, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String saveAs(@RequestBody Map<String, String> body){
        logger.info("save as ...");
        try {
            PrintWriter writer = new PrintWriter(currentPath + "/" + body.get("newName"), "UTF-8");
            writer.print(new String(DatatypeConverter.parseBase64Binary(body.get("fileContent"))));
            writer.close();
        }
        catch (IOException e) {
            logger.error("saveas: " + e.getMessage());
            e.printStackTrace();
        }
        return "success";

    }

    @RequestMapping(value="/filesystem/files/list", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, Object> listFiles(){
        File folder = new File(currentPath);
        Map<String, Object> result = new HashMap<>();

        Map<String, Boolean> files = new HashMap<>();
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null && listOfFiles.length > 0) {
            for (int i = 0; i < listOfFiles.length; i++) {
                files.put(listOfFiles[i].getName(), listOfFiles[i].isDirectory());
            }
        }

        result.put("files", files);
        return result;
    }

    @RequestMapping(value="/properties/save", produces={MediaType.TEXT_PLAIN_VALUE})
    String saveProperties(@RequestParam(value="propertiesFile") String propertiesFile){
        try {
            ConsoleFoundation.PROPERTIES.setPropertiesFile( new File(propertiesFile));
            ConsoleFoundation.PROPERTIES.save();
        }
        catch (ConsoleException ce) {
            ce.printStackTrace();
            return ce.getMessage();
        }
        return "success";
    }

    @RequestMapping(value="/filesystem/directory/change", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String> changeDirectory(@RequestParam(value="newPath") String newPath,
                                        @RequestParam(value="save", required=false) Boolean save){
        Map<String, String> result = new HashMap<>();
        currentPath = newPath.replace('\\', '/');
        if (save) {
            try {
                ConsoleFoundation.PROPERTIES.setDistributionDirectory(new Directory(new File(newPath)));
                ConsoleFoundation.PROPERTIES.save();
            }
            catch (ConsoleException ce) {
                ce.printStackTrace();
            } catch (Directory.DirectoryException de) {
                de.printStackTrace();
            }
        }
        result.put("newPath", currentPath);
        return result;
    }

    @RequestMapping(value="/filesystem/file/download", method = RequestMethod.GET)
    ResponseEntity<Resource> downloadFile(@RequestParam(value="logFile", required=false) String logFile) throws IOException{
        File file = new File(logFile);
        Path path = Paths.get(file.getAbsolutePath());
        Resource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
            .contentLength(file.length())
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(resource);
    }

    @RequestMapping(value="/agents/start", method = RequestMethod.POST, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String startNewAgent() {
        new Thread() {
            public void run() {
                Grinder.main(new String[] {});
            }
        }.start();
        return "success";
    }
}