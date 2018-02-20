chartTitles = ["Transactions Per Second", "Average Response Time(ms)", "Response Time Standard Deviation(ms)"]
chartIndexes = [4, 2, 3]
MAX_NUMBER_SAMPLE = 720
GRAPH_SAMPLE = 5
charts = []

function loadFiles(data) {
    serverFiles = data.files;
    var listFile = document.getElementById("listFile");
    folders = files = "";
    for (var file in serverFiles) {
        if (serverFiles[file] == true) {
            folders = folders + '\n<li class="folder"><a onClick="browseFolder(this)" href="#">'+file+'</a></li>';
        } else {
            files = files + '\n<li class="file"><a onClick="openFile(this)" href="#">'+file+'</a></li>';
        }
    }
    listFile.innerHTML = '<li class="folder"><a onClick="browseFolder(this)" href="#">..</a></li>' + folders + files;
}

function browseFolder(e) {
    // Set new Path
    part11 = document.getElementById("currentPath").innerText;
    part21 = e.text;
    if (part21 == "..") {
        path = part11.substring(0, part11.lastIndexOf('/'));
    }
    else {
        path = part11 + "/" + part21;
    }
    $.getJSON('/filesystem/directory/change', {newPath: path}, function(data) {
        document.getElementById("currentPath").innerText = data.newPath;
        $.getJSON('/filesystem/files/list', {}, function(data) {
            loadFiles(data);
        });
    });
}

function setBasePath() {
    // Set new Path
    path = document.getElementById("basePath").value;
    $.getJSON('/filesystem/directory/change', {newPath: path}, function(data) {
        document.getElementById("currentPath").innerText = data.newPath;
        $.getJSON('/filesystem/files/list', {}, function(data) {
            loadFiles(data);
        });
    });
}

function openFile(e) {
    file = e.text;
    if (file.indexOf("/") < 0 && file.indexOf("\\") < 0) {
        file = document.getElementById("currentPath").innerText + '/' + file
    }
    $.getJSON('/filesystem/files', {
        file: file
    }, function(data) {
        $("#currentFile").text(data.filePath);
        editor.getDoc().setValue(data.doc);
    });
}

function loadLogs(data) {
    document.getElementById("adressLog").innerText = data.logPath;
    var idLogs = document.getElementById("idLog");
    idLogs.innerHTML = "";
    for (var file in data.logFiles) {
        idLogs.innerHTML = idLog.innerHTML + '\n<li class="file"><a onClick="openLogFile(this)" href="#">'+file+'</a></li>';
    }
}

function openLogFile(e) {
    var logFolder = document.getElementById("adressLog").innerText;
    var log = logFolder + '/' + e.text;
    downloadButton = document.getElementById("downloadfile");
    downloadButton.href = "/filesystem/file/download?logFile="+encodeURIComponent(log);
    downloadButton.download = e.text;

    $.getJSON('/logs', {
        logFile: log
    }, function(data) {
        document.getElementById("loglog").value = (data.doc);
    });
}


function notify(message) {
  if (!("Notification" in window)) {
    alert("Your Browser doesn't support notifications");
  }
  // Voyons si l'utilisateur est OK pour recevoir des notifications
  else if (Notification.permission === "granted") {
    // Si c'est ok, créons une notification
    var notification = new Notification(message, {
        body: 'Please wait ...',
        icon: '/img/logo.png'
    });
  }
  // Sinon, nous avons besoin de la permission de l'utilisateur
  // Note : Chrome n'implémente pas la propriété statique permission
  // Donc, nous devons vérifier s'il n'y a pas 'denied' à la place de 'default'
  else if (Notification.permission !== 'denied') {
    Notification.requestPermission(function (permission) {

      // Quelque soit la réponse de l'utilisateur, nous nous assurons de stocker cette information
      if(!('permission' in Notification)) {
        Notification.permission = permission;
      }

      // Si l'utilisateur est OK, on crée une notification
      if (permission === "granted") {
        var notification = new Notification(message, {
            body: ' Please wait ... ',
            icon: '/img/logo.png'
        });
      }
    });
  }
}


function resetCharts() {
    for (chartId = 0; chartId < charts.length; ++chartId) {
        charts[chartId].data.datasets.splice(0, charts[chartId].data.datasets.length)
        charts[chartId].data.labels.splice(0, charts[chartId].data.labels.length)
    }
    charts.splice(0, charts.length);
    for (chartId = 0; chartId < chartTitles.length; ++chartId) {
        canvas = document.getElementById('myChart' + chartId).getContext("2d");
        canvas.height = 100;
        var option = {
            showLines: true,
            animation: false,
            elements: {
              point: {
                radius: 0
              }
            },
            scales: {
                yAxes: [{
                    gridLines: {
                        color: '#434343',
                        zeroLineColor: '#434343',
                        zeroLineWidth: 2
                    }
                }],
                xAxes: [{
                    gridLines: {
                        color: '#434343',
                        zeroLineColor: '#434343',
                        zeroLineWidth: 2
                    },
                    scaleLabel: {
                        display: true,
                        fontSize: 20,
                        labelString: "" + chartTitles[chartId]
                    }
                }]
            }
        };
        myLineChart = Chart.Line(canvas, {
            data: {labels: [], datasets: []},
            options: option
        });
        myLineChart.update();
        charts.push(myLineChart);
    }
    sampleId = 0
    graphInitialized = false
}

$(document).ready(function() {
    alertfirefox=false

    $.getJSON('/filesystem/files/list', {}, function(data) {
        loadFiles(data);
    });

    $.getJSON('/properties', {}, function(data) {
        document.getElementById("currentPath").innerText = data.distributionDirectory;
        document.getElementById("basePath").value = data.distributionDirectory;
        document.getElementById("selectedPropertiesFile").innerHTML =
            "<a onClick='openFile(this)' href='#'>" + data.propertiesFile + "</a>";
    });

    $.getJSON('/logs/list', {}, function(data) {
        loadLogs(data)
    });

    $.get('/version', {}, function(data) {
       $("#versionGrinder").text(data);
    });

    editor = CodeMirror.fromTextArea(document.getElementById("demotext"), {
        lineNumbers: true,
    });

    $("#refreshPeriod").attr("value", 1000);
    resetCharts();
    refreshGrinder(1000);
});

function updateResultTable(data) {
    // Add the columns
    $("#dataHead").empty();
    var newRow = document.createElement('tr');
    newRow.innerHTML = '<tr><th class="resultTitle">Test Name</th>'
    //alert(JSON.stringify(data))
    for (i = 0; i < data.columns.length; i++) {
        newRow.innerHTML += '<th class="result"> ' + data.columns[i] + '</th>'
    }
    newRow.innerHTML += '</tr>'
    document.getElementById('dataHead').appendChild(newRow);

    // Add the test results
    $("#datakid").empty();
    for (j = 0; j < data.tests.length; j++) {
        var newRow = document.createElement('tr');
        newRow.innerHTML = '<tr><td class="resultTitle" id="tabtest' + j + '">' + data.tests[j].description + '</td>'
        for (i = 0; i < data.columns.length; i++) {''
            newRow.innerHTML += '<td class="result" id="tabtests' + j + '">' + roundToTwo(data.tests[j].statistics[i]) + '</td>'
        }
        newRow.innerHTML += '</tr>'
        document.getElementById('datakid').appendChild(newRow);
    }
    // Add a "total" line
    var newRow = document.createElement('tr');
    newRow.innerHTML = '<tr><td class="resultTitle">Total</td>'
    for (i = 0; i < data.totals.length; i++) {''
        newRow.innerHTML += '<td class="result">' + roundToTwo(data.totals[i]) + '</td>'
    }
    newRow.innerHTML += '</tr>'
    document.getElementById('datakid').appendChild(newRow);

    // update dashboard elements
    errorRate = 0
    if (data.totals[0] > 0)
        Math.round(data.totals[1]/data.totals[0]*100)
    document.getElementById('dashboard_error_rate').innerText = roundToTwo(errorRate);
    document.getElementById('dashboard_tps').innerText = roundToTwo(data.totals[4]);
}

function updateResultGraphs(data) {
    if (data.tests.length > 0) {
        d = new Date();
        curTime = d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds();
        if (!graphInitialized) {
            graphSampleId = 0;
            graphInitialized = true;
            for (curveIndex = 0; curveIndex < data.tests.length; curveIndex++) {
                curveColor = "rgba(" +
                               (Math.floor(Math.random() * 155) + 100) + ", " +
                               (Math.floor(Math.random() * 155) + 100) + ", " +
                               (Math.floor(Math.random() * 155) + 100) + ", 1)"
                for (chartId = 0; chartId < charts.length; chartId++) {
                    charts[chartId].data.labels[graphSampleId] = curTime;
                    charts[chartId].data.datasets.push({
                       label: data.tests[curveIndex].description,
                       borderColor: curveColor,
                       pointBackgroundColor: curveColor,
                       data: [ ]
                    })
                }
            }
        }

        for (chartId = 0; chartId < charts.length; chartId++) {
            for (cases = 0; cases < data.tests.length; cases++) {
                if (charts[chartId].data.datasets[cases] != null) {
                    statId = chartIndexes[chartId]
                    plotValue = data.tests[cases].statistics[statId];
                    if (charts[chartId].data.datasets[cases].data.length > MAX_NUMBER_SAMPLE) {
                        charts[chartId].data.datasets[cases].data.shift()
                    }
                    charts[chartId].data.datasets[cases].data[graphSampleId] = plotValue
                }
            }
            if (charts[chartId].data.labels.length > MAX_NUMBER_SAMPLE) {
                charts[chartId].data.labels.shift()
            }
            charts[chartId].data.labels[graphSampleId] = curTime;
            charts[chartId].update();
        }
        if (graphSampleId < MAX_NUMBER_SAMPLE) {
            graphSampleId++
        }
    }
}

function refreshGrinder(refreshPeriod) {
        interval = setInterval(function() {
            $.getJSON('/agents/status', {}, function(data) {
                $("#kids-body").empty();
                nbOfAgents = data.length;

                document.getElementById('dashboard_agents').innerText = nbOfAgents;

                if (nbOfAgents > 0) {
                    for (agentIndex = 0; agentIndex < nbOfAgents; agentIndex++) {
                        agent = data[agentIndex];
                        if (agent.workers.length == 0) {
                            agent.state = "CONNECTED";
                            setProcessState(document.getElementById('processState'), 'start');
                        }
                        else {
                           setProcessState(document.getElementById('processState'), 'stop');
                        }
                        var newRow = document.createElement('tr');
                        newRow.innerHTML = '<td class="result"      id="agent'   + agentIndex + '">' + agent.name +
                                           '</td><td class="result" id="states'  + agentIndex + '">' + agent.state +
                                           '</td><td class="result" id="workers' + agentIndex + '">' + agent.workers.length +
                                           '</td>';
                        document.getElementById('kids-body').appendChild(newRow);
                    }
                }
                else {
                    var newRow = document.createElement('tr');
                    newRow.innerHTML = '<tr><td></td><td class="result">No agents connected.</td><td></td><td id="workers"></td><br>';
                    document.getElementById('kids-body').appendChild(newRow);
                }
            });

            $.getJSON('/recording/data' + ($('#checkboxD').is(":checked")?'-latest':''), {}, function(data) {
                updateResultTable(data)
                if (sampleId == 0) {
                    updateResultGraphs(data);
                }
                sampleId = (sampleId + 1) % GRAPH_SAMPLE

            });
        }, refreshPeriod);
    }


$(function() {
    $('#setPath').bind('click', function() {
        path = document.getElementById("currentPath").innerText;
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ distributionDirectory: path }),
            url: '/properties',
            dataType: "json"
        });
    });
});

$(function() {
    $('#refreshlog').bind('click', function() {
        $.getJSON('/logs/list', {}, function(data) {
           loadLogs(data)
        });
    });
    return false;
});

$(function() {
    $('#te').bind('click', function() {
        savePath = document.getElementById("currentFile").innerText;
        if (confirm("You will change the content of your file, sure ? ")) {
            $.getJSON('/filesystem/files/write', {
                fileContent: editor.getValue(),
                filePath: savePath
            }, function(data) {
                alert(data.error)
            });
        }
    });
    return false;
});


$(function() {
    $('#saveas').bind('click', function() {
        var saveName = prompt("Choose the name of your file", "");
        if (saveName != null) {
            $.getJSON('/filesystem/files/save', {
                newName: saveName,
                fileContent: editor.getValue()
            }, function(data) {
                $.getJSON('/filesystem/files/list', {}, function(data) {
                    loadFiles(data);
                });
             });
        }
        return false;
    });
});

function startAfterDistribution(status) {
    if (status.state == "started" || status.state == "sending") {
        $.getJSON('/files/status', {}, function(data) {
            startAfterDistribution(data["last-distribution"]);
        });
    }
    else if (status.state == "finished") {
            $.post('/recording/stop', {}, function(data) {
                $.post('/recording/start');
            })
            $.post('/agents/start-workers', {}, function(data) {
                if (data == "success") {
                    notify('Test starting...');
                } else {
                    // TODO replace by http://jqueryui.com/dialog/
                    alert("Error: Unable to start worker processes, please check Grinder logs for more information");
               }
            });
            return false;
        }
    else {
        // TODO replace by http://jqueryui.com/dialog/
        alert("Error: Unable to distribute files, please check Grinder logs for more information");
    }
}

$(function() {
    $('#processCtrl').bind('click', function() {
        running = document.getElementById("processState").src.indexOf("stop") >= 0
        if (running) {
            $.post('/agents/stop-workers', {}, function(data) {
                if (data == "success") {
                    notify('Test stopped');
                }
                else {
                    // TODO replace by http://jqueryui.com/dialog/
                    alert("Error: Unable to stop worker processes, please check Grinder logs for more information");
                }
            });
        }
        else {
            $.post('/files/distribute', {}, function(data) {
                if (data.state == "started") {
                    notify('File distribution...');
                    $.getJSON('/files/status', {}, startAfterDistribution(data));
                } else {
                    // TODO replace by http://jqueryui.com/dialog/
                    alert("Error: Unable to start the file distribution, please check Grinder logs for more information");
                }
        }, "json");
        }
    });
});

$(function() {
    $('#restartrecord').bind('click', function() {
        $.post('/recording/zero', {}, function(data) {
            resetCharts();
        });
    });
});

$(function() {
    $('#startAgent').bind('click', function() {
        $.post('/agents/start', {}, function(data) {
            if (data != "success") {
                // TODO replace by http://jqueryui.com/dialog/
                alert("Error: Unable to start embedded Agent, please check Grinder logs for more information.");
            }
        });
    });
});

$(function() {
    $('#deletelog').bind('click', function() {
        $.ajax({
            type: 'DELETE',
            url: '/logs/delete',
            success: function(data) {
                if (data.error == "ok") {
                    $("#idLog").empty();
                    document.getElementById("loglog").value = "";
                }
                else {
                    // TODO replace by http://jqueryui.com/dialog/
                    alert("Error: Unable to delete the local logs: " + data.error);
                }
            },
            dataType: "json"
        });

        $.getJSON('/logs/list', {}, function(data) {
           loadLogs(data)
        });
    });
});


function showElem(e) {
  var tabToShow = e.href.substring(e.href.lastIndexOf('#'));
  var elements = document.getElementsByClassName("tab-pane");
  for(var i=0; i < elements.length; i++) {
      if (elements[i].id == tabToShow) {
        elements[i].style.display = "block";
      }
      else {
        elements[i].style.display = "none";
      }
  }
}

function setProcessState(e, state) {
    var image = e.src;
    if (state == 'stop') {
        e.src = image.replace   ('start', 'stop');
    }
    else {
        e.src = image.replace('stop', 'start');
    }
}


function showChart(index) {
    for (chartId = 0; chartId < charts.length; chartId++) {
        display = "none";
        if (chartId == index) {
            display = "block";
        }
        document.getElementById("myChart"+chartId).style.display = display;
    }
}

function changeTempo() {
    refreshPeriod = document.getElementById("refreshPeriod").value;
    clearInterval(interval);
    refreshGrinder(refreshPeriod);
}

function setPropertiesFile() {
    var tess = document.getElementById("currentFile").innerText;
    if (tess != "") {
        document.getElementById("selectedPropertiesFile").innerHTML = "<a onClick='openFile(this)' href='#'>" + tess + "</a>";
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ propertiesFile: tess }),
            url: '/properties',
            dataType: "json"
        });
    }

    if (tess == "") {
        alert("please choose file");
    }
}

function roundToTwo(num) {
    result = +(Math.round(num + "e+2")  + "e-2");
    return isNaN(result)?0:result
}