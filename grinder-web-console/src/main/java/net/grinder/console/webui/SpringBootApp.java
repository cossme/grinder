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
@EnableAutoConfiguration
public class SpringBootApp {

    private String currentPath;
    private String logFile;
    private String propertiesFile;
    private short testFinish;
    private GrinderProperties properties;

    SpringBootApp () {
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
        logFile = "";
        testFinish = 0;
    }

    @RequestMapping(value="/_setPath", produces={"application/json"})
    @ResponseBody
    String setPath(@RequestParam(value="newchem2", required=true) String paths2){
        WebConsoleUI.getInstance().logger.error(new Object(){}.getClass().getEnclosingMethod().getName() + ": NOT IMPLEMENTED");
        return "{\"path2\": \"" + paths2 + "\"}";
    }

    @RequestMapping(value="/_logServ", produces={"application/json"})
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

    @RequestMapping(value="/_startGathering", produces={"application/json"})
    @ResponseBody
    String startRecup(){
        WebConsoleUI.getInstance().logger.info("reset data  ...");
        WebConsoleUI.getInstance().model.reset();
        return "{\"startrecordi\": 200}";
    }

    @RequestMapping(value="/_release", produces={"application/json"})
    @ResponseBody
    String release(){
        return "{\"versionGR\": \"" + GrinderBuild.getVersionString() + "\"}";
    }

    @RequestMapping(value="/_zeroStats", produces={"application/json"})
    @ResponseBody
    String stopRe(){
        WebConsoleUI.getInstance().logger.info("zero data  ...");
        WebConsoleUI.getInstance().model.zeroStatistics();
        return "{\"startrecordi\": 200}";
    }

    @RequestMapping(value="/_stopGathering", produces={"application/json"})
    @ResponseBody
    String stopGathering(){
        WebConsoleUI.getInstance().logger.info("stop data  ...");
        WebConsoleUI.getInstance().model.stop();
        return "{\"stopre\": 200}";
    }

    @RequestMapping(value="/_setDistributionPath", produces={"application/json"})
    @ResponseBody
    String setDistributionPath(){
        try {
            WebConsoleUI.getInstance().consoleProperties.setAndSaveDistributionDirectory(new Directory(new File(currentPath)));
            return "{\"erreur\": \"ok\"}";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "{\"erreur\": \"" + e.getMessage() + "\"}";
        }
    }

    @RequestMapping("/_getLog")
    @ResponseBody
    String getLog(@RequestParam(value="doclo", required=true) String logFile){
        String contentFile = "";
        //String filePath = properties.getProperty("grinder.logDirectory") + "/" + logFile;
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

    @RequestMapping(value="/_getFile", produces={"application/json"})
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
        output += " \"erreur\": \"" + error +"\"\n";
        output += "}";
        return output;
    }

    @RequestMapping(value="/_writeFile", produces={"application/json"})
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
        return "{\"erreur\": \"" + err + "\"}";
    }

    @RequestMapping(value="/_saveAs", produces={"application/json"})
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

    @RequestMapping(value="/_gatherData", produces={"application/json"})
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

    @RequestMapping(value="/_agentStats", produces={"application/json"})
    @ResponseBody
    String agentStats(){
        int numberOfAgents = WebConsoleUI.getInstance().processControl.getNumberOfLiveAgents();

        String output = "{\n \"adressFichierlog\": \"" + logFile + "\",\n  \"allWorker\": [\n   0\n  ],\n \"erreur\": \"ok\",\n";
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

    @RequestMapping(value="/_listFiles", produces={"application/json"})
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

    @RequestMapping(value="/_changeDir", produces={"application/json"})
    @ResponseBody
    String changeChem2(@RequestParam(value="newPath2", required=true) String newPath){
        currentPath = newPath.replace('\\', '/');
        return "{\"Pathes2\": \"" + currentPath + "\"}";
    }

    @RequestMapping(value="/_postDistribution", produces={"application/json"})
    @ResponseBody
    String postDistribution(){
        String err = "ok";
        try {
            WebConsoleUI.getInstance().fileDistribution.scanDistributionFiles();
            final FileDistributionHandler distributionHandler = WebConsoleUI.getInstance().fileDistribution.getHandler();
            FileDistributionHandler.Result result = distributionHandler.sendNextFile();
            while (result != null) {
                result = distributionHandler.sendNextFile();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            err = e.getMessage();
        }
        String output = "{\n";
        output += " \"distribute\": 200,\n";
        output += " \"erreur\": \"" + err + "\"\n";
        output += "}";
        return output;
    }

    @RequestMapping(value="/_deleteLogs", produces={"application/json"})
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
        return "{\"erreur\": \"" + err + "\"}";
    }

    @RequestMapping(value="/_downloadFile", method = RequestMethod.GET)
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

    @RequestMapping(value="/_startWorkers", produces={"application/json"})
    @ResponseBody
    String startWorkers(){
        try {
            properties.setAssociatedFile(new File(
                    WebConsoleUI.getInstance().consoleProperties.getPropertiesFile().getAbsolutePath().substring(currentPath.length()+1)));
            WebConsoleUI.getInstance().processControl.startWorkerProcesses(properties);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "{\"rep\": \"" + e.getMessage() +"\"}";
        }
        return "{\"rep\": \"success\", \"statuwor\": 200}";
    }

    @RequestMapping(value="/_stopAgents", produces={"application/json"})
    @ResponseBody
    String stopAgents(){
        WebConsoleUI.getInstance().processControl.resetWorkerProcesses();
        return "{\"response\": \"success\"}";
    }

    @RequestMapping(value="/_grinderVersion", produces={"application/json"})
    @ResponseBody
    String grinderVersion(){
        return "{\"versionG\": \"" + GrinderBuild.getVersionString() + "\"}";
    }

    @RequestMapping(value="/_resetLogServ", produces={"application/json"})
    @ResponseBody
    String resetLogServ(@RequestParam(value="resetlog", required=true) String resetlog){
        this.logFile=resetlog;
        WebConsoleUI.getInstance().logger.info("setting/reset the logaddress inside the Python global variable");

        return "{ \"filei\":1}";
    }

    @RequestMapping(value="/_setPropertiesFileLocation", produces={"application/json"})
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

    @RequestMapping(value="/_newAgent", produces={"application/json"})
    @ResponseBody
    String newAgent() {
        /*new Thread() {
            public void run() {
                WebConsoleUI.getInstance().logger.info("Starting an embedded agent...");
                String separator = System.getProperty("file.separator");
                String classpath = System.getProperty("java.class.path");
                String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
                try {
                    new ProcessBuilder(path, "-cp",
                            classpath,
                            Grinder.class.getName()).start();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();*/
        Grinder.main(new String[] {});
        return "{ \"erreur\":\"ok\"}";
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
