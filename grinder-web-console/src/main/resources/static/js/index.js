chartTitles = ["Transactions Per Second", "Average Response Time(ms)", "Response Time Standard Deviation(ms)"]
chartIndexes = [ 4, 2, 3]
charts = []

function loadFiles(data) {
    fich1 = data.docss2;
    var listFile = document.getElementById("listFile");
    folders = files = "";
    for (var files2 in fich1) {
        if (fich1[files2] == true) {
            folders = folders + '\n<li class="folder"><a onClick="browseFolder(this)" href="#">'+files2+'</a></li>';
        } else {
            files = files + '\n<li class="file"><a onClick="openFile(this)" href="#">'+files2+'</a></li>';
        }
    }
    listFile.innerHTML = '<li class="folder"><a onClick="browseFolder(this)" href="#">..</a></li>' + folders + files;
}

function browseFolder(e) {
    // Set new Path
    part11 = document.getElementById("currentPath").innerText;
    part21 = e.text;
    if (part21 == "..") {
        chemVal2 = part11.substring(0, part11.lastIndexOf('/'));
    }
    else {
        chemVal2 = part11 + "/" + part21;
    }
    $.getJSON('//filesystem/directory/change', {newPath2: chemVal2}, function(data) {
        document.getElementById("currentPath").innerText = data.Pathes2;
        $.getJSON('/filesystem/files/list', {}, function(data) {
                loadFiles(data);
        });
    });
}

function setBasePath() {
    // Set new Path
    chemVal2 = document.getElementById("basePath").value;
    $.getJSON('//filesystem/directory/change', {newPath2: chemVal2}, function(data) {
        document.getElementById("currentPath").innerText = data.Pathes2;
        $.getJSON('/filesystem/files/list', {}, function(data) {
                loadFiles(data);
        });
    });
}

function openFile(e) {
    file = e.text;
    if (file.indexOf("/") >= 0) {
        file = file.substring(file.lastIndexOf("/")+1)
    }
    $.getJSON('/filesystem/files', {
        docAdvan: file
    }, function(data) {
        $("#currentFile").text(data.doc1);
        editor.getDoc().setValue(data.doc);
    });
}

function loadLogs(data) {
    document.getElementById("adressLog").innerText = data.logPath;
    var idLogs = document.getElementById("idLog");
    idLogs.innerHTML = "";
    for (var files2 in data.doclog) {
        idLogs.innerHTML = idLog.innerHTML + '\n<li class="file"><a onClick="openLogFile(this)" href="#">'+files2+'</a></li>';
    }
}

function openLogFile(e) {
    var logFolder = document.getElementById("adressLog").innerText;
    var log = logFolder + '/' + e.text;
    downloadButton = document.getElementById("downloadfile");
    downloadButton.href = "/filesystem/file/download?logFile="+encodeURIComponent(log);
    downloadButton.download = e.text;

    $.getJSON('/logs', {
        doclo: log
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

    initialize = 0
}

$(document).ready(function() {
    alertfirefox=false
    statCheckbox=false
    statuscheck()

    $.getJSON('/filesystem/files/list', {}, function(data) {
        loadFiles(data);
    });

    $.getJSON('/_gatherData', { statCheckbox :statCheckbox}, function(data) {
        document.getElementById("currentPath").innerText = data.chem2;
        document.getElementById("basePath").value = data.chem2;
        $("#setmsg2").attr("value", data.initPath2);
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

    // The following section is certainly to be cleaned
    i = 0;
    initialize = 0
    nombreTest = 0;
    ili = 0;
    fich = [];
    nombreFich = 1;

    timee = ""
    ik = 0
    tempo = 1000
    $("#changeTime").attr("value", tempo);

    resetCharts();

    interv(tempo);

});

function interv(tempo) {
     runningTest=0;
        intervalle = setInterval(function() {
            statuscheck()
            chemSave = document.getElementById("currentFile").innerText;

            $.getJSON('/_agentStats', {}, function(data) {
                $("#kids-body").empty();
                nombrAgent = data.nombreAgents;
                document.getElementById('dashboard_agents').innerText = nombrAgent;

                if (data.nombreAgents > 0) {
                    for (j = 0; j < data.nombreAgents; j++) {
                        if (data.textagent[j].workers == "")  {
                            data.textagent[j].workers = "[ ]"
                            data.textagent[j].state = "Connected"
                            setProcessState(document.getElementById('processState'), 'start');
                        }
                        else if (data.textagent[j].workers != "[ ]") {
                           data.textagent[j].state = "Running"
                           setProcessState(document.getElementById('processState'), 'stop');
                        }

                        var newRow = document.createElement('tr');
                        newRow.innerHTML = ' <td class="result" id="agent' + j + '">' + data.textagent[j].name +
                                           ' </td><td class="result" id="states' + j + '">' + data.textagent[j].state + '<br>';
                        ale = data.textagent[j].workers[0].name;
                        if (ale) {
                            if (runningTest == 0) {
                              notify('Test running')
                              runningTest=1;
                            }

                            mesWorkers = data.textagent[j].workers;
                            nombreWorkers = mesWorkers.length;

                            newRow.innerHTML = newRow.innerHTML + '<td class="result" id="workers' + j +
                                                '"> workers received:' + data.allWorker[j] + '</td><br>';

                            document.getElementById('kids-body').appendChild(newRow);
                        }
                        else {
                            newRow.innerHTML = newRow.innerHTML + '<td class="result" id="workers' + j + '">'
                                                                + data.textagent[j].workers + '</td><br>';
                            document.getElementById('kids-body').appendChild(newRow);
                        }
                    }
                }
                else {
                    var newRow = document.createElement('tr');
                    newRow.innerHTML = '<tr><td></td><td class="result">No agents connected.</td><td></td><td id="workers"></td><br>';
                    document.getElementById('kids-body').appendChild(newRow);
                }
            });

            $.getJSON('/_gatherData', { statCheckbox : statCheckbox }, function(data) {
                nombreTest = data.tailla;
                $("#dataProperties").text(data.pathsSend);
                document.getElementById("selectedPropertiesFile").innerHTML = "<a onClick='openFile(this)' href='#'>" + data.etoilefile + "</a>";
                document.getElementById("currentPath").innerText = data.chem2;
                $("#setmsg2").attr("placeholder", data.initPath2);
                $("#datakid").empty();
                for (j = 0; j < nombreTest; j++) {
                    var newRow = document.createElement('tr');
                    newRow.innerHTML = '<tr> <td class="result" id="tabtest' + j + '"> ' + data.resu[j].description +
                                       '</td><td class="result" id="tabtests ' + j + '">' + data.resu[j].statistics[0] +
                                       '</td><td class="result" id="taberrors' + j + '">' + data.resu[j].statistics[1] +
                                       '</td><td class="result" id="tabaverage' + j + '">' + (Math.round((data.resu[j].statistics[2]) * 100) / 100) +
                                       '</td><td class="result" id="tabtts' + j + '">' + (Math.round((data.resu[j].statistics[3]) * 100) / 100) +
                                       '</td><td class="result" id="tabtps' + j + '">' + (Math.round((data.resu[j].statistics[4]) * 100) / 100) +
                                       '</td><td class="result" id="tabpeak' + j + '">' + data.resu[j].statistics[5] + '</td><br></tr>';
                    document.getElementById('datakid').appendChild(newRow);
                }

                totalNbTests  = data.glob[0];
                totalNbErrors = data.glob[1];
                totalTps      = (Math.round((data.glob[4]) * 100) / 100);
                var newRow = document.createElement('tr');
                newRow.innerHTML = '<tr> <td class="result">Total</td><td class="result">' + totalNbTests +
                                   '</td><td class="result">' + totalNbErrors +
                                   '</td><td class="result">' + (Math.round((data.glob[2]) * 100) / 100) +
                                   '</td><td class="result">' + (Math.round((data.glob[3]) * 100) / 100) +
                                   '</td><td class="result">' + totalTps +
                                   '</td><td class="result">' + data.glob[5] + '</td></tr><br>';

                document.getElementById('datakid').appendChild(newRow);
                errorRate = 0
                if (totalNbTests != 0)
                    Math.round(totalNbErrors/totalNbTests*100)
                document.getElementById('dashboard_error_rate').innerText = errorRate;
                document.getElementById('dashboard_tps').innerText = totalTps;

                $("#tab0").text(data.resu[0]);
                $("#tab1").text(data.resu[1]);
                $("#tab2").text(data.resu[2]);
                $("#tab3").text(data.resu[3]);
                $("#tab4").text(data.resu[4]);
                $("#tab5").text(data.resu[5]);

                d = new Date();
                timee = d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds();
                if (nombreTest > 0) {
                    if (initialize == 0) {
                        ik = 0;
                        initialize = 1;
                        for (nombrecourb = 0; nombrecourb < nombreTest; nombrecourb++) {
                            curveColor = "rgba(" +
                                           (Math.floor(Math.random() * 155) + 100) + ", " +
                                           (Math.floor(Math.random() * 155) + 100) + ", " +
                                           (Math.floor(Math.random() * 155) + 100) + ", 1)"
                            for (chartId = 0; chartId < charts.length; chartId++) {
                                charts[chartId].data.labels[ik] = timee;
                                charts[chartId].data.datasets.push({
                                   label: data.resu[nombrecourb].description,
                                   borderColor: curveColor,
                                   pointBackgroundColor: curveColor,
                                   data: [ ]
                                })
                            }
                        }
                    }

                    running = document.getElementById("processState").src.indexOf("stop") >= 0
                    if (running) {
                        for (chartId = 0; chartId < charts.length; chartId++) {
                            for (cases = 0; cases < nombreTest; cases++) {
                                if (charts[chartId].data.datasets[cases] != null) {
                                    statId = chartIndexes[chartId]
                                    valuegraph = data.resu[cases].statistics[statId];
                                    charts[chartId].data.datasets[cases].data[ik] = valuegraph
                                }
                            }  
                            charts[chartId].data.labels[ik] = timee;
                            charts[chartId].update();
                        }
                        ik++
                    }
                    else {
                        ik = 0
                    }
                }
                i = i + 1;
            });
        }, tempo);
    }


$(function() {
    $('#setPath').bind('click', function() {
        path = document.getElementById("currentPath").innerText;
            $.getJSON('/_setDistributionPath', { distributionPath: path }, function(data) {
            if ((data.error) != "ok") {
              alert(data.error + "  if the problem persists, please refresh the page ")
            }
        });
        return false;
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
        chemSave = document.getElementById("currentFile").innerText;
        if (confirm("You will change the content of your file, sure ? ")) {
            $.getJSON('/filesystem/files/write', {
                ajaa: editor.getValue(),
                chemup: chemSave
            }, function(data) {
                alert(data.error)
            });
        }
    });
    return false;
});


$(function() {
    $('#saveas').bind('click', function() {
        var nomSave = prompt("Choose the name of your file", "");
        if (nomSave != null) {
            $.getJSON('/filesystem/files/save', {
                newname: nomSave,
                ajaa: editor.getValue()
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
            $.post('/recording/reset');
            $.post('/agents/start-workers', {}, function(data) {
                if (data == "success") {
                    notify('Starting test...');
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
            if (data == "success") {
                $("#datakid").empty();
                resetCharts();
            } else {
                // TODO replace by http://jqueryui.com/dialog/
                alert("Error: Unable to zeroing the statistics, please check Grinder logs for more information.");
            }
        });
        return false;
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
    tempo = document.getElementById("changeTime").value;
    clearInterval(intervalle);
    interv(tempo);
}

function setPropertiesFile() {
    var tess = document.getElementById("currentFile").innerText;
    if (tess != "") {
        document.getElementById("selectedPropertiesFile").innerHTML = "<a onClick='openFile(this)' href='#'>" + tess + "</a>";
        $.getJSON('/_setPropertiesFileLocation', {
            a: tess
        }, function(data) {});
    }

    if (tess == "") {
        alert("please choose file");
    }
}

function statuscheck() {
    if ($('#checkboxD').is(":checked"))  {
        statCheckbox=true
    }
    else {
        statCheckbox=false
    }
}