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
    $.getJSON('/_changeDir', {newPath2: chemVal2}, function(data) {
        document.getElementById("currentPath").innerText = data.Pathes2;
        $.getJSON('/_listFiles', {}, function(data) {
                loadFiles(data);
        });
    });
}

function setBasePath() {
    // Set new Path
    chemVal2 = document.getElementById("basePath").value;
    $.getJSON('/_changeDir', {newPath2: chemVal2}, function(data) {
        document.getElementById("currentPath").innerText = data.Pathes2;
        $.getJSON('/_listFiles', {}, function(data) {
                loadFiles(data);
        });
    });
}

function openFile(e) {
    file = e.text;
    if (file.indexOf("/") >= 0) {
        file = file.substring(file.lastIndexOf("/")+1)
    }
    $.getJSON('/_getFile', {
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
    var idLogs = document.getElementById("adressLog").innerText;
    document.getElementById("downloadfile").href = "/_downloadFile?logFile="+encodeURI(idLogs);
    var log = idLogs + '/' + e.text;
    $.getJSON('/_getLog', {
        doclo: log
    }, function(data) {
        document.getElementById("loglog").value = (data.doc);
    });
}

$(document).ready(function() {

    alertfirefox=false
    statCheckbox=false

    statuscheck()

    $.getJSON('/_listFiles', {}, function(data) {
        loadFiles(data);
    });

    $.getJSON('/_gatherData', { statCheckbox :statCheckbox}, function(data) {
        document.getElementById("currentPath").innerText = data.chem2;
        document.getElementById("basePath").value = data.chem2;
        $("#setmsg2").attr("value", data.initPath2);
        systeme=data.systeme
    });

    $.getJSON('/_logServ', {}, function(data) {
        loadLogs(data)
    });

    $.getJSON('/_grinderVersion', {}, function(data) {
       $("#versionGrinder").text("The Grinder v"+data.versionG);
    });

    var elementlog = document.getElementById('idLog');

    elementlog.addEventListener('dblclick', function() {

        if (alertfirefox == false) {
            ua = navigator.userAgent;
            nav=ua.indexOf("Firefox");

            if (nav > 0) {
                alert("You can download only one Logfile cause of Firefox, please use Chrome for a best usage or refresh your cache everytime")
                alertfirefox=true
            }
        }

        log = document.getElementById("idLog").value;

        document.getElementById("downloadfile").href = "/_downloadFile?logFile="+log;

        if (log == "") {

            alert("please choose a file")

        } else {
            $.getJSON('/_traitelog', {
                doclo: log
            }, function(data) {

                document.getElementById("loglog").value = (data.doc);
            });
        }
    });


    document.getElementById("workerr").disabled = true;
    editor = CodeMirror.fromTextArea(document.getElementById("demotext"), {
        lineNumbers: true,
    });

    indexcourbetot = false
    indexcourbe = false
    i = 0;
    fortest = 0
    nombreTest = 0;
    ili = 0;
    fich = [];
    nombreFich = 1;

    timee = ""
    ik = 1
    tempo = 1000
    $("#changeTime").attr("value", tempo);

    canvas1 = document.getElementById('myChart8');
    canvas1.height = 100;
    data1 = {
        labels: ["Average time"],
        datasets: []
    };

    var option = {
        showLines: true,
        scales: {
            yAxes: [{
                gridLines: {
                    color: 'rgba(240, 231, 213, 1)',
                    zeroLineColor: 'rgba(240, 231, 213, 1)',
                    zeroLineWidth: 2
                }
            }],
            xAxes: [{
                gridLines: {
                    color: 'rgba(240, 231, 213, 1)',
                    zeroLineColor: 'rgba(240, 231, 213, 1)',
                    zeroLineWidth: 2
                }
            }]
        }
    };

    myLineChart = Chart.Line(canvas1, {
        data: data1,
        options: option
    });

    myLineChart.update();

    canvas2 = document.getElementById('myChart9');
    canvas2.height = 110;
    data2 = {
        labels: ["TTSD"],
        datasets: []
    };

    myLineChart1 = Chart.Line(canvas2, {
        data: data2,
        options: option
    });

    myLineChart1.update();

    canvas3 = document.getElementById('myChart10');var ctx = $('#myChart');
    canvas3.height = 110;

    data3 = {
        labels: ["TPS"],
        datasets: []
    };

    myLineChart2 = Chart.Line(canvas3, {
        data: data3,
        options: option
    });

    myLineChart2.update();

    interv(tempo);

});

function interv(tempo) {
     runningTest=0;
        intervalle = setInterval(function() {

            // logarea();
            statuscheck()
            wayout = document.getElementById("currentFile").innerHTML;
            chemSave = document.getElementById("currentFile").innerText;


            if (wayout != "") {

                document.getElementById("te").disabled = false;
            } else {

                document.getElementById("te").disabled = true;
            }




            $.getJSON('/_agentStats', {}, function(data) {

                $("#kids-body").empty();

                indexcourbetot = false
                indexcourbe = false

                nombrAgent = data.nombreAgents;

                if (data.nombreAgents > 0) {

                    document.getElementById("testdistribution").disabled = false;
                } else {

                    document.getElementById("testdistribution").disabled = true;
                }

                // alert(nombrAgent);

                selectedPropFile = document.getElementById("selectedPropertiesFile").innerText;

                if (nombrAgent > 0) {


                    if (selectedPropFile != "No file selected ...") {

                        document.getElementById("workerr").disabled = false;


                    }

                }

                if (nombrAgent == 0) {

                    document.getElementById("workerr").disabled = true;
                }

                if (selectedPropFile == "No file selected ...") {

                    document.getElementById("workerr").disabled = true;

                }




                if (data.nombreAgents > 0) {



                    for (j = 0; j < data.nombreAgents; j++) {



                        if (data.textagent[j].workers == "")

                        {

                            data.textagent[j].workers = "[ ]"
                            data.textagent[j].state = "Connected"

                        }



                        if (data.textagent[j].workers != "[ ]") {

                           data.textagent[j].state = "Running"

                        }





                        if (data.textagent[j].workers == "[ ]") {


                            indexcourbe = false;
                        } else {

                            indexcourbe = true;
                        }

                        if ((indexcourbetot || indexcourbe) == true)

                        {

                            indexcourbetot = true;

                        } else

                        {
                            indexcourbetot = false;

                        }



                        var newRow = document.createElement('tr');

                        newRow.innerHTML = ' </td> <td id="agent' + j + '">' + data.textagent[j].name + ' </td><td id="states' + j + '">' + data.textagent[j].state + '<br>';

                        //newRow.innerHTML = '<td id="workers'+j+'">' + data.textagent[j].workers + '</td><br>';

                        ale = data.textagent[j].workers[0].name;

                        if (ale) {

                            if (runningTest == 0) {



                                                                if (!("Notification" in window)) {
                                            alert("Your Browser doesn't support notifications");
                                          }

                                          // Voyons si l'utilisateur est OK pour recevoir des notifications
                                          else if (Notification.permission === "granted") {
                                            // Si c'est ok, créons une notification
                                            var notification = new Notification('Test running ', {

                                                icon: '/logo.png'
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
                                                var notification = new Notification('Test running ', {

                                                icon: '/logo.png'
                                            });
                                              }
                                            });
                                          }

                                             runningTest=1;




                            }


                             $.getJSON('/_statusTest', {

                            }, function(data) {

                            });


                            mesWorkers = data.textagent[j].workers
                            nombreWorkers = mesWorkers.length

                            newRow.innerHTML = newRow.innerHTML + '<td id="workers' + j + '"> workers received:' + data.allWorker[j] + '</td><br>';

                            document.getElementById('kids-body').appendChild(newRow);

                        } else {

                            $.getJSON('/_statusNotif', {

                            }, function(data) {

                                    if (data.testNotif == 1) {

                                        runningTest=0;


                                                                if (!("Notification" in window)) {
                                            alert("Your Browser doesn't support notifications");
                                          }

                                          // Voyons si l'utilisateur est OK pour recevoir des notifications
                                          else if (Notification.permission === "granted") {
                                            // Si c'est ok, créons une notification
                                            var notification = new Notification('Test finished', {

                                                body: 'You can now consult your results',
                                                icon: '/logo.png'
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
                                                var notification = new Notification('Test finished', {

                                                body: 'You can now consult your results',
                                                icon: '/logo.png'
                                            });
                                              }
                                            });
                                          }




                                    }






                            });



                            newRow.innerHTML = newRow.innerHTML + '<td id="workers' + j + '">' + data.textagent[j].workers + '</td><br>';

                            document.getElementById('kids-body').appendChild(newRow);

                        }

                    }




                }

                if (data.nombreAgents == 0) {

                    var newRow = document.createElement('tr');

                    newRow.innerHTML = '<tr><td></td><td class="result">No agents started .. </td><td></td><td id="workers"></td><br>';

                    document.getElementById('kids-body').appendChild(newRow);

                }

            });




            $.getJSON('/_gatherData', { statCheckbox : statCheckbox }, function(data) {

                nombreTest = data.tailla
                $("#dataProperties").text(data.pathsSend);
                document.getElementById("selectedPropertiesFile").innerHTML = "<a onClick='openFile(this)' href='#'>" + data.etoilefile + "</a>";

                document.getElementById("currentPath").innerText = data.chem2;

                $("#setmsg2").attr("placeholder", data.initPath2);

                $("#datakid").empty();

                for (j = 0; j < nombreTest; j++) {

                    var newRow = document.createElement('tr');

                    newRow.innerHTML = '<tr><td class="result" id="tabtest' + j + '"> ' + data.resu[j].description +
                                       '</td><td class="result" id="tabtests ' + j + '">' + data.resu[j].statistics[0] +
                                       '</td><td class="result" id="taberrors' + j + '">' + data.resu[j].statistics[1] +
                                       '</td><td class="result" id="tabaverage' + j + '">' + (Math.round((data.resu[j].statistics[2]) * 100) / 100) +
                                       '</td><td class="result" id="tabtts' + j + '">' + (Math.round((data.resu[j].statistics[3]) * 100) / 100) +
                                       '</td><td class="result" id="tabtps' + j + '">' + (Math.round((data.resu[j].statistics[4]) * 100) / 100) +
                                       '</td><td class="result" id="tabpeak' + j + '">' + data.resu[j].statistics[5] + '</td><br></tr>';

                    document.getElementById('datakid').appendChild(newRow);
                }

                var newRow = document.createElement('tr');
                newRow.innerHTML = '<tr><td class="result">Total</td><td class="result">' + data.glob[0] +
                                   '</td><td class="result">' + data.glob[1] +
                                   '</td><td class="result">' + (Math.round((data.glob[2]) * 100) / 100) +
                                   '</td><td class="result">' + (Math.round((data.glob[3]) * 100) / 100) +
                                   '</td><td class="result">' + (Math.round((data.glob[4]) * 100) / 100) +
                                   '</td><td class="result">' + data.glob[5] + '</td></tr><br>';

                document.getElementById('datakid').appendChild(newRow);

                $("#tab0").text(data.resu[0]);
                $("#tab1").text(data.resu[1]);
                $("#tab2").text(data.resu[2]);
                $("#tab3").text(data.resu[3]);
                $("#tab4").text(data.resu[4]);
                $("#tab5").text(data.resu[5]);

                d = new Date();

                timee = d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds();


                if (nombreTest > 0) {


                    if (fortest == 0) {


                        fortest = 1;


                        for (nombrecourb = 0; nombrecourb < nombreTest; nombrecourb++) {


                            colorcourbe1 = (Math.random()) * 1000000

                            colorcourbe1 = Math.round(colorcourbe1)

                            coulourrgb = colorcourbe1.toString()

                            while (coulourrgb.length < 6) {

                                coulourrgb = "0" + coulourrgb;

                            }


                            myLineChart.data.labels[ik] = timee;

                            datadd = {
                                label: data.resu[nombrecourb].description,

                                lineTension: 0.1,
                                borderColor: "#" + coulourrgb + "",
                                borderCapStyle: 'butt',
                                borderDash: [],
                                borderDashOffset: 0.0,
                                borderJoinStyle: 'miter',
                                pointBackgroundColor: "#fff",
                                pointBorderWidth: 1,
                                pointHoverRadius: 5,
                                pointHoverBorderWidth: 2,
                                pointRadius: 0.3,
                                pointHitRadius: 1,
                                data: []
                            }

                            myLineChart.data.datasets.push(datadd)




                            myLineChart1.data.labels[ik] = timee;

                            datadd1 = {
                                label: data.resu[nombrecourb].description,

                                lineTension: 0.1,
                                borderColor: "#" + coulourrgb + "",
                                borderCapStyle: 'butt',
                                borderDash: [],
                                borderDashOffset: 0.0,
                                borderJoinStyle: 'miter',
                                pointBackgroundColor: "#fff",
                                pointBorderWidth: 1,
                                pointHoverRadius: 5,
                                pointHoverBorderWidth: 2,
                                pointRadius: 0.3,
                                pointHitRadius: 1,
                                data: []
                            }

                            myLineChart1.data.datasets.push(datadd1)


                            myLineChart2.data.labels[ik] = timee;

                            datadd2 = {
                                label: data.resu[nombrecourb].description,

                                lineTension: 0.1,
                                borderColor: "#" + coulourrgb + "",
                                borderCapStyle: 'butt',
                                borderDash: [],
                                borderDashOffset: 0.0,
                                borderJoinStyle: 'miter',
                                pointBackgroundColor: "#fff",
                                pointBorderWidth: 1,
                                pointHoverRadius: 5,
                                pointHoverBorderWidth: 2,
                                pointRadius: 0.3,
                                pointHitRadius: 1,
                                data: []
                            }

                            myLineChart2.data.datasets.push(datadd2)

                        }

                    }


                    if (indexcourbetot == true) {


                        for (cases = 0; cases < nombreTest; cases++) {
                            valuegraph2 = data.resu[cases].statistics[2];
                            myLineChart.data.datasets[cases].data[ik] = valuegraph2

                            valuegraph3 = data.resu[cases].statistics[3];
                            myLineChart1.data.datasets[cases].data[ik] = valuegraph3

                            valuegraph4 = data.resu[cases].statistics[4];
                            myLineChart2.data.datasets[cases].data[ik] = valuegraph4
                        }


                        myLineChart.data.labels[ik] = timee;
                        myLineChart.update();


                        myLineChart1.data.labels[ik] = timee;
                        myLineChart1.update();


                        myLineChart2.data.labels[ik] = timee;
                        myLineChart2.update();


                        ik = ik + 1
                    }

                }

                i = i + 1;
            });

        }, tempo);



    }



$(function() {
    $('#setPath').bind('click', function() {
        path = document.getElementById("currentPath").innerText;
        $.getJSON('/_setDistributionPath',
                  {
                    distributionPath: path
                  },
                  function(data) {
                    if ((data.erreur) != "ok") {
                      alert(data.erreur + "  if the problem persists, please refresh the page ")
                    }

                  }
                 );
        return false;
    });
});

$(function() {
    $('#refreshlog').bind('click', function() {
        $.getJSON('/_logServ', {}, function(data) {
           loadLogs(data)
        });
    });
    return false;
});

$(function() {
    $('#te').bind('click', function() {

        chemSave = document.getElementById("currentFile").innerText;

        if (confirm("You will change the content of your file, sure ? ")) {

            $.getJSON('/_writeFile', {
                ajaa: editor.getValue(),
                chemup: chemSave



            }, function(data) {


                alert(data.erreur)




            });

        }
    });

    return false;

});


$(function() {
    $('#saveas').bind('click', function() {

        var nomSave = prompt("Choose the name of your file", "");

        if (nomSave != null) {

            $.getJSON('/_saveAs', {
                newname: nomSave,
                ajaa: editor.getValue()
            }, function(data) {

            });

        }

        return false;
    });
});

$(function() {
    $('#testdistribution').bind('click', function() {


        $.getJSON('/_postDistribution', {

        }, function(data) {

            if (data.distribute == 200) {

                alert("Files distributed correctly");

            } else {
                alert("files not distributed");
            }

        });



        return false;
    });
});


$(function() {
    $('#changePath2').bind('click', function() {


        chemValback2 = document.getElementById("currentPath").innerText;

        if (chemValback2 == "") {
            alert("Path empty");
        } else {

            lettr2 = (chemValback2.length) - 1;

            lettreSortie = chemValback2[0]

            while (chemValback2[lettr2] != ('/')) {

                if (chemValback2[lettr2] == "\\") {
                    chemValback2 = chemValback2.substring(0, lettr2 + 1)
                    break;
                }


                chemValback2 = chemValback2.substring(0, lettr2)
                lettr2 = lettr2 - 1


                if (chemValback2 == "C:") {
                    chemValback2 = "C:\\"
                }

                if (chemValback2 == "c:") {
                    chemValback2 = "C:\\"
                }


                if (lettr2 < 1) {

                    if (chemValback2 == "C:") {
                        chemValback2 = "C:\\"
                    }

                    if (chemValback2 == "c:") {
                        chemValback2 = "C:\\"
                    }
                    break;

                }



            }


            lettr2 = (chemValback2.length) - 1;
            chemValback2 = chemValback2.substring(0, lettr2)
            if (chemValback2 == "C:") {
                chemValback2 = "C:\\"
            }

            if (chemValback2 == "c:") {
                chemValback2 = "C:\\"
            }
        }


        if (chemValback2 != "") {



            $.getJSON('/_changeDir', {
                newPath2: chemValback2
            }, function(data) {
                $.getJSON('/_listFiles', {}, function(data) {

                    $("#idListAdvan").empty();
                    fich1 = data.docss2


                    var Listd1 = document.getElementById("idListAdvan");
                    ilil = 0
                    for (var files2 in fich1) {

                        if (fich1[files2] == true) {


                            Listd1.options[ilil] = new Option(files2)
                            Listd1.options[ilil].style.color = "darkblue"


                        } else {


                            Listd1.options[ilil] = new Option(files2)
                            Listd1.options[ilil].style.color = "darkgrey"


                        }


                        ilil = ilil + 1
                    }

                    document.getElementById("currentPath").innerText = data.chemfile
                });

            });



        } else {
            if (lettreSortie == "C") {
                chemValback2 = "C:\\"
            }

            if (lettreSortie == "c") {
                chemValback2 = "C:\\"
            } else {

                chemValback2 = "/"
            }
            $.getJSON('/_changeDir', {
                newPath2: chemValback2
            }, function(data) {

                $.getJSON('/_listFiles', {}, function(data) {


                    $("#idListAdvan").empty();
                    fich1 = data.docss2


                    var Listd1 = document.getElementById("idListAdvan");
                    ilil = 0
                    for (var files2 in fich1) {

                        if (fich1[files2] == true) {


                            Listd1.options[ilil] = new Option(files2)
                            Listd1.options[ilil].style.color = "darkblue"


                        } else {


                            Listd1.options[ilil] = new Option(files2)
                            Listd1.options[ilil].style.color = "darkgrey"


                        }


                        ilil = ilil + 1
                    }

                    document.getElementById("currentPath").innerText = data.chemfile
                });

            });

        }

        return false;
    });
});

$(function() {
    $('#workerr').bind('click', function() {
        $.getJSON('/_postDistribution', {}, function(data) {
            if (data.distribute == 200) {
              if (!("Notification" in window)) {
                alert("Your Browser doesn't support notifications");
              }
              // Voyons si l'utilisateur est OK pour recevoir des notifications
              else if (Notification.permission === "granted") {
                // Si c'est ok, créons une notification
                var notification = new Notification('File distributed ', {
                    body: ' Please wait ...',
                    icon: '/logo.png'
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
                    var notification = new Notification('File distributed ', {
                        body: ' Please wait ... ',
                        icon: '/logo.png'
                    });
                  }
                });
              }

              setTimeout(function() {

                  if (nombreTest > 0) {

                      fortest = 0;
                      ik = 1

                      delete myLineChart
                      delete myLineChart1
                      delete myLineChart2

                      canvas1 = document.getElementById('myChart8');
                      data1 = {

                          labels: ["Average Time"],
                          datasets: [

                          ]
                      };

                      var option = {
                          showLines: true,

                      };

                      myLineChart = Chart.Line(canvas1, {
                          data: data1,
                          options: option

                      });

                      myLineChart.update();


                      canvas2 = document.getElementById('myChart9');
                      data2 = {

                          labels: ["TTSD"],
                          datasets: [

                          ]
                      };

                      var option2 = {
                          showLines: true,

                      };

                      myLineChart1 = Chart.Line(canvas2, {
                          data: data2,
                          options: option2

                      });

                      myLineChart1.update();


                      canvas3 = document.getElementById('myChart10');
                      data3 = {

                          labels: ["TPS"],
                          datasets: [

                          ]
                      };

                      var option3 = {
                          showLines: true,

                      };

                      myLineChart2 = Chart.Line(canvas3, {
                          data: data3,
                          options: option3

                      });

                      myLineChart2.update();

                      $.getJSON('/_startGathering', {}, function(data) {});

                  }

                  $.getJSON('/_startWorkers', {}, function(data) {
                      $("#staworker").text(data.rep);
                      if (data.statuwor != 200) {
                          alert("error in launching the test");
                      } else {
                        if (!("Notification" in window)) {
                          alert("Your Browser doesn't support notifications");
                        }
                        // Voyons si l'utilisateur est OK pour recevoir des notifications
                        else if (Notification.permission === "granted") {
                          // Si c'est ok, créons une notification
                          var notification = new Notification('Test started ', {
                              body: ' In progress ...',
                              icon: '/logo.png'
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
                              var notification = new Notification('Test started ', {
                                  body: ' In progress ... ',
                                  icon: '/logo.png'
                              });
                            }
                          });
                        }
                     }
                  });
                  return false;
              }, 1000);
            } else {
                alert("files not distributed")
            }
        });
    });
});



$(function() {
    $('#sworkerr').bind('click', function() {
        $.getJSON('/_stoworker', {}, function(data) {
            if (data.statusstop == "200") {
              if (!("Notification" in window)) {
                alert("Your Browser doesn't support notifications");
              }
              // Voyons si l'utilisateur est OK pour recevoir des notifications
              else if (Notification.permission === "granted") {
                // Si c'est ok, créons une notification
                var notification = new Notification('Test stopped', {

                    icon: '/logo.png'
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
                    var notification = new Notification('Test stopped ', {

                        icon: '/logo.png'
                    });
                  }
                });
              }
            }
        });
        return false;
    });
});

$(function() {
    $('#stoprecord').bind('click', function() {
        $.getJSON('/_stopGathering', {}, function(data) {
            if (data.stopre != 200) {
                alert("Recording stopped not correctly");
            } else {
                alert("Recording stopped correctly");
            }
        });
        return false;
    });
});

$(function() {
    $('#restartrecord').bind('click', function() {
        $.getJSON('/_zeroStats', {}, function(data) {
            if (data.startrecordi != 200) {
                alert("Recording restarted not correctly  ");
            } else {
                $("#datakid").empty();
            }
        });
        return false;
    });
});

$(function() {
    $('#Browseproperties').bind('click', function() {
        rapido = document.getElementById("idList").value;
        if (rapido == "") {
            alert("please choose a file")
        } else {
            $.getJSON('/_opandtake', {
                docdoc: rapido
            }, function(data) {
                $("#nofilesproperties").text(data.propertiess);
                $("#nofiles").text(data.fileins);
                $("#areainfo").text(data.docss);
                $("#ru").val(data.runss);
                $("#thr").val(data.threadss);
                $("#pro").val(data.processess);
            });
        }
    });
});


$(function() {
    $('#startAgent').bind('click', function() {
        $.getJSON('/_newAgent', {}, function(data) {
            if (data.erreur != "ok") {
                alert("Error: " + data.erreur);
            }
        });
    });
});



$(function() {
    $('#deletelog').bind('click', function() {
        $.getJSON('/_deleteLogs', {}, function(data) {
            if (data.erreur == "ok") {
                $("#idLog").empty();
                document.getElementById("loglog").value = "";
            }
            else {
                alert("Error: " + data.erreur);
            }
        });
        $.getJSON('/_resetLogServ', { resetlog:""}, function(data) {
        });
        $.getJSON('/_logServ', {}, function(data) {
           loadLogs(data)
        });
    });
});


$(function() {
    $('#stopAgents').bind('click', function() {
        $.getJSON('/_stopAgents', {}, function(data) {});
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

function switchIcon(e) {
    var image = e.src;
    if (image.indexOf('start') >= 0) {
        e.src = image.replace   ('start', 'stop');
    }
    else {
        e.src = image.replace('stop', 'start');
    }
}



function logElem() {
    document.getElementById("barre").style.width = "23%";
    document.getElementById("mainpanel").style.width = "75%";
    document.getElementById("barre").style.visibility = "visible";
    document.getElementById("advan").style.display = "none";
    document.getElementById("LogMode").style.display = "block";
    // document.getElementById("updating-chart").style.display = "none";
}

function graphideElem() {
    document.getElementById("barre").style.visibility = "hidden";
    // document.getElementById("updating-chart").style.display = "block";
}

function changeElem() {
    document.getElementById("barre").style.width = "23%";
    document.getElementById("mainpanel").style.width = "75%";
    document.getElementById("barre").style.visibility = "visible";
    // document.getElementById("updating-chart").style.display = "none";
    document.getElementById("advan").style.display = "block";
}


function courbe1() {
    document.getElementById("myChart8").style.display = "block";
    document.getElementById("myChart9").style.display = "none";
    document.getElementById("myChart10").style.display = "none";
}

function courbe2() {
    document.getElementById("myChart8").style.display = "none";
    document.getElementById("myChart9").style.display = "block";
    document.getElementById("myChart10").style.display = "none";
}

function courbe3() {
    document.getElementById("myChart8").style.display = "none";
    document.getElementById("myChart9").style.display = "none";
    document.getElementById("myChart10").style.display = "block";
}

function changeTempo() {
    tempo = document.getElementById("changeTime").value;
    clearInterval(intervalle);
    interv(tempo);
}

function setPropertiesFile() {
    var tess = document.getElementById("currentFile").innerText;
    if (tess != "") {
        document.getElementById("selectedPropertiesFile").innerHTML = "<a onClick='openFile(this)' href='#'>" + data.etoilefile + "</a>";
        $.getJSON('/_setPropertiesFileLocation', {
            a: tess
        }, function(data) {});
    }

    if (tess == "") {
        alert("please choose file");
    }
}

function statuscheck() {
    if ($('#checkboxD').is(":checked"))
    {
        statCheckbox=true
    }
    else
    {
        statCheckbox=false
    }
}


function logarea() {
    $.getJSON('/_log', {

    }, function(data) {
        $("#loglog").text(data.loglog);
    });
}



function notifyMeStop() {
  // Voyons si le navigateur supporte les notifications
  if (!("Notification" in window)) {
    alert("Your Browser doesn't support notifications");
  }

  // Voyons si l'utilisateur est OK pour recevoir des notifications
  else if (Notification.permission === "granted") {
    // Si c'est ok, créons une notification
    var notification = new Notification('Test finished', {
        
        icon: '/logo.png'
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
        var notification = new Notification('Test finished ', {
        
        icon: '/logo.png'
    });
      }
    });
  }

  // Comme ça, si l'utlisateur a refusé toute notification, et que vous respectez ce choix,
  // il n'y a pas besoin de l'ennuyer à nouveau.
}




