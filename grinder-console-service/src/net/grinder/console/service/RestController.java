

package net.grinder.console.service;

import net.grinder.common.GrinderBuild;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.model.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Created by solcyr on 27/01/2018.
 */
@Controller
public class RestController {

    static Processes processes = new Processes();
    static Files     files     = new Files();
    static Recording recording = new Recording();

    /**
     * Returns the version of The Grinder.
     * @return the version of The Grinder.
     */
    @RequestMapping(value="/version", method = RequestMethod.GET, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String getVersion(){
        return GrinderBuild.getName();
    }

    /**
     * Returns the current values of the console options.
     * @return the current values of the console options.
     */
    @RequestMapping(value="/properties", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String> getProperties(){
        ConsoleProperties properties = Bootstrap.getInstance().consoleProperties;
        return Properties.getProperties(properties);
    }

    /**
     * Set console options. The body of the request should be a map of keys to new values; you can provide some
     * or all of the properties. A map of the keys and their new values will be returned. You can find out the
     * names of the keys by issuing a GET to /properties.
     * @param newProperties new Properties to save
     * @return A map of the keys and their new values
     */
    @RequestMapping(value="/properties", method = RequestMethod.PUT, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String> setProperties(@RequestBody Map<String, String> newProperties){
        ConsoleProperties properties = Bootstrap.getInstance().consoleProperties;
        return Properties.setProperties(properties, newProperties);
    }

    /**
     * Save the current console options in the preferences file. The preferences file is called .grinder_console and
     * is stored in the home directory of the user account that is used to run the console.
     */
    @RequestMapping(value="/properties/save", method = RequestMethod.POST, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String saveProperties(){
        ConsoleProperties properties = Bootstrap.getInstance().consoleProperties;
        return Properties.save(properties);
    }


    /**
     * Returns the status of the agent and worker processes.
     * @return status list
     */
    @RequestMapping(value="/agents/status", method = RequestMethod.GET, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    List<Report> getAgentStatus(){
        return processes.status();
    }

    /**
     * Send a stop signal to connected worker processes. Equivalent to the reset processes button.
     */
    @RequestMapping(value="/agents/stop", method = RequestMethod.POST, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String stoptAgents(){
        return processes.stopAgents();
    }

    /**
     * Send a start signal to the agents to start worker processes. Equivalent to the start processes button.
     */
    @RequestMapping(value="/agents/start-workers", method = RequestMethod.POST, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String startWorkers(@RequestBody Map<String, String> newProperties){
        ConsoleProperties properties = Bootstrap.getInstance().consoleProperties;
        return processes.startWorkers(properties, newProperties);
    }

    /**
     * Send a stop signal to connected worker processes. Equivalent to the reset processes button.
     */
    @RequestMapping(value="/agents/stop-workers", method = RequestMethod.POST, produces={MediaType.TEXT_PLAIN_VALUE})
    @ResponseBody
    String stopWorkers(){
        return processes.stopWorkers();
    }

    /**
     * Start the distribution of files to agents that have an out of date cache. Distribution may take some time,
     * so the service will return immediately and the files will be distributed in proceeds in the background.
     * The service returns a map with an :id entry that can be used to identify the particular distribution request.
     */
    @RequestMapping(value="/files/distribute", method = RequestMethod.POST, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    DistributionResult distributeFiles(){
        return files.startDistribution(Bootstrap.getInstance().fileDistribution);
    }

    /**
     * Returns whether the agent caches are stale (i.e. they are out of date with respect to the console's central
     * copy of the files), and the status of the last file distribution.
     */
    @RequestMapping(value="/files/status", method = RequestMethod.GET, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, Object> getFileStatus(){
        return files.status(Bootstrap.getInstance().fileDistribution);
    }

    /**
     * Return the current recorded data. Equivalent to the data in the results tab.
     */
    @RequestMapping(value="/recording/data", method = RequestMethod.GET, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, Object> getRecordingData(){
        return recording.data();
    }

    /**
     * Return the latest sample of recorded data. Equivalent to the data in the lower pane of the results tab.
     */
    @RequestMapping(value="/recording/data-latest", method = RequestMethod.GET, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, Object> getLatestRecordingData(){
        return recording.dataLatest();
    }

    /**
     * Start capturing data. An initial number of samples may be ignored, depending on the configured console options.
     */
    @RequestMapping(value="/recording/start", method = RequestMethod.POST, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String> startRecording(){
        return recording.start();
    }

    /**
     * Stop the data capture.
     */
    @RequestMapping(value="/recording/stop", method = RequestMethod.POST, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String> stopRecording(){
        return recording.stop();
    }

    /**
     * Return the current recording status.
     */
    @RequestMapping(value="/recording/status", method = RequestMethod.GET, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String> getRecordingStatus(){
        return recording.status();
    }

    /**
     * Discard all recorded data. After a reset, the model loses all knowledge of Tests; this can be useful when
     * swapping between scripts. It makes sense to reset with the worker processes stopped.
     */
    @RequestMapping(value="/recording/reset", method = RequestMethod.POST, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String>  resetRecordingStatus(){
        return recording.reset();
    }

    /**
     * Reset the recorded data values to zero.
     */
    @RequestMapping(value="/recording/zero", method = RequestMethod.POST, produces={MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    Map<String, String>  zeroRecordingStatus(){
        return recording.zero();
    }

}
