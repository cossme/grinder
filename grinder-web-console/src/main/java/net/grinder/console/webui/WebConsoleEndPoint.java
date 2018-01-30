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
import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.distribution.FileDistributionHandler;
import net.grinder.statistics.*;
import net.grinder.util.Directory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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

    @RequestMapping(value="/filesystem/files", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String getFile(@RequestParam(value="docAdvan", required=true) String fileName){
        String contentFile = "";
        String error = "ok";
        String filePath = currentPath + "/" + fileName;
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
        output += " \"doc1\": \"" + filePath + "\",\n";
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
        // If the properties file has been update, we reset it.
        if (filePath.equals(this.propertiesFile)){
            setPropertiesFileLocation(this.propertiesFile);
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

    @RequestMapping(value="/_setDistributionPath", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String setDistributionPath(){
        try {
            WebConsoleUI.getInstance().consoleProperties.setAndSaveDistributionDirectory(new Directory(new File(currentPath)));
            return "{\"error\": \"ok\"}";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping(value="/_gatherData", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String gatherData(@RequestParam(value="statCheckbox", required=false) String statusCheckbox){
        WebConsoleUI.getInstance().logger.info("recover Data");
        String propertiesFile = WebConsoleUI.getInstance().consoleProperties.getPropertiesFile().getAbsolutePath().replace('\\', '/');
        double totalPeakTps = 0;

        String output = "{\n";
        output += "\"chem\": \"/\",\n";
        output += "\"chem2\": \"" + currentPath + "\",\n";
        output += "\"etoilefile\": \"" + propertiesFile + "\",\n";
        output += "\"initPath\": \"\",\n";
        output += "\"initPath2\": \"" + currentPath + "\",\n";
        output += "\"pathsSend\": \"" + propertiesFile + "\",\n";
        output += "\"resu\": [ ";

        int nbOfTest = 0;
        StatisticsServices statisticsServices = StatisticsServicesImplementation.getInstance();
        StatisticsView view = statisticsServices.getSummaryStatisticsView();
        Number[] totalStats = new Number[view.getExpressionViews().length];

        PeakStatisticExpression peakTPSExpression =
                statisticsServices.getStatisticExpressionFactory().createPeak(
                        statisticsServices.getStatisticsIndexMap().getDoubleIndex("peakTPS"),
                        statisticsServices.getTPSExpression());

        for (int j = 0; j < view.getExpressionViews().length; ++j) {
            final StatisticExpression expression = view.getExpressionViews()[j].getExpression();
            if (expression.isDouble()) {
                totalStats[j] = new Double(0);
            }
            else {
                totalStats[j] = new Long(0);
            }
        }
        if (WebConsoleUI.getInstance().testIndex != null) {
            nbOfTest = WebConsoleUI.getInstance().testIndex.getNumberOfTests();

            for (int  i = 0; i < WebConsoleUI.getInstance().testIndex.getNumberOfTests(); ++i) {
                Test test = WebConsoleUI.getInstance().testIndex.getTest(i);
                StatisticsSet cumulativeStats = WebConsoleUI.getInstance().testIndex.getCumulativeStatistics(i);
                StatisticsSet stats;
                if ("true".equalsIgnoreCase(statusCheckbox)) {
                    stats = WebConsoleUI.getInstance().testIndex.getLastSampleStatistics(i);
                } else {
                    stats = cumulativeStats;
                }

                output += "\n{\"test\":" + test.getNumber() +
                        ",\"description\":\"" + test.getDescription() +
                        "\",\"statistics\":[";
                for (int j = 0; j < view.getExpressionViews().length; ++j) {
                    final StatisticExpression expression = view.getExpressionViews()[j].getExpression();
                    output += "\"";
                    if (expression.isDouble()) {
                        double value =  expression.getDoubleValue(stats);
                        totalStats[j] = totalStats[j].doubleValue() + value;
                        output += value;
                    }
                    else {
                        long value =  expression.getLongValue(stats);
                        totalStats[j] = totalStats[j].longValue() + value;
                        output += value;
                    }
                    output += "\",";
                }

                double peakTps = peakTPSExpression.getLongValue(cumulativeStats);
                output += "\"" + peakTps  + "\"]},";
                totalPeakTps += peakTps;
            }
        }
        output = output.substring(0, output.length() - 1);
        output += "\n],\n";
        output += "\"glob\": [\n";
        if (nbOfTest > 0) {
            for (int j = 0; j < totalStats.length; ++j) {
                output += "\"" + totalStats[j] + "\",\n";
            }
            output += "\"" + totalPeakTps + "\"\n";
            output += "],\n";
        }
        else {
            output += "\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"],\n";
        }
        output += "\"tailla\": " + nbOfTest + "\n }";
        return output;
    }

    @RequestMapping(value="/_agentStats", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String agentStats(){
        int numberOfAgents = WebConsoleUI.getInstance().processControl.getNumberOfLiveAgents();

        String output = "{\n  \"allWorker\": [\n   0\n  ],\n \"error\": \"ok\",\n";
        output += " \"nombreAgents\": " + numberOfAgents + ",\n";
        output += " \"textagent\": [\n";

        //loop agents
        if (WebConsoleUI.getInstance().m_processReports != null) {
            for (int i = 0; i < WebConsoleUI.getInstance().m_processReports.length; ++i) {
                ProcessReport.State agentState = WebConsoleUI.getInstance().m_processReports[i].getAgentProcessReport().getState();

                AgentIdentity aIdentity = WebConsoleUI.getInstance().m_processReports[i].getAgentProcessReport().getAgentIdentity();
                output += " {\n";
                output += "  \"id\": \"" + aIdentity.getUniqueID() + "\",\n";
                output += "  \"name\": \"" + aIdentity.getName() + "\",\n";
                output += "  \"number\": " + aIdentity.getNumber() + ",\n";
                output += "  \"state\": \"" + agentState + "\",\n";
                output += "  \"workers\": [ ";
                for (int j = 0; j < WebConsoleUI.getInstance().m_processReports[i].getWorkerProcessReports().length; ++j) {
                    WorkerProcessReport report = WebConsoleUI.getInstance().m_processReports[i].getWorkerProcessReports()[j];
                    WorkerIdentity wIdentity = report.getWorkerIdentity();
                    output += "\n {\n";
                    output += "  \"id\": \"" + wIdentity.getUniqueID() + "\",\n";
                    output += "  \"name\": \"" + wIdentity.getName() + "\",\n";
                    output += "  \"number\": " + wIdentity.getNumber() + "\n";
                    output += " },";
                }
                output = output.substring(0, output.length() - 1);
                output += "  ]\n";
                output += " }\n";
            }
        }
        output += " ]\n}\n";
        return output;
    }

    @RequestMapping(value="/_setPropertiesFileLocation", produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    String setPropertiesFileLocation(@RequestParam(value="a", required=true) String propertiesFile){
        WebConsoleUI.getInstance().logger.info("setting properties file location");
        this.propertiesFile=propertiesFile.replace('\\', '/');
        File propFile = new File(propertiesFile);
        try {
            WebConsoleUI.getInstance().consoleProperties.setAndSavePropertiesFile(propFile);
        }
        catch (ConsoleException e) {
            e.printStackTrace();
            WebConsoleUI.getInstance().consoleProperties.setPropertiesFile(propFile);
        }
        try {
            this.properties = new GrinderProperties(WebConsoleUI.getInstance().consoleProperties.getPropertiesFile());
        }
        catch (GrinderProperties.PersistenceException e) {
            e.printStackTrace();
            this.properties = new GrinderProperties();
        }
        return "{ \"filei\":1}";
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
