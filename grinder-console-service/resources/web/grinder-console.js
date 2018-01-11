jQuery(function($) {

    function addChangeDetection(scope) {
        var changeables = $(".changeable", scope);

        if (!changeables.length) {
            return;
        }

        $("label", scope).each(function() {
            var l = this;

            if (l.htmlFor != '') {
                var e = $("#" + l.htmlFor, scope)[0];

                if (e) {
                    e.label = this;
                } else {
                    $("[name='" + l.htmlFor + "']", scope).each(function() {
                        this.label = l;
                    });
                }
            }
        });

        jQuery.fn.visible = function(show) {
            return this.css("visibility", show ? "visible" : "hidden");
        };

        var submit = $("#set-properties", scope);
        submit.visible(false);

        submit.closest("form").keyup(function(event) {
            if (event.keyCode == 13) {
                submit.click();
            }
        });

        changeables.each(function() {
            if (this.type === "checkbox") {
                this.modified = function() {
                    return this.checked != this.defaultChecked;
                };
            } else {
                var original = this.value;
                this.modified = function() {
                    return original != this.value;
                };
            }

            $(this).change(function(e) {
                // This is wrong if multiple controls share the same label.
                $(e.target.label).toggleClass("changed", e.target.modified());

                submit.visible(changeables.filter(function(x) {
                    return this.modified();
                }).length);
            });
        });
    }

    function createPoller(e) {

        var tokens = {}; // key => token
        var listeners = {}; // key => {listeners}
        var xhr = null;

        var poller = {
            poll : function() {
                if (xhr != null) {
                    xhr.abort();
                    xhr = null;
                }

                if ($.isEmptyObject(tokens)) {
                    return;
                }

                var p = this;

                xhr = $.getJSON("/ui/poll", tokens);

                xhr.then(function(x) {
                    $.each(x, function(_k, v) {
                        if (tokens.hasOwnProperty(v.key)) {
                            $.each(listeners[v.key],
                                   function() { this(v.key, v.value); });

                            tokens[v.key] = v.next;
                        }
                        else {
                            console.warn("Ignoring value with unknown key", v);
                        }
                    });
                })
                .then(function() {
                    p.poll();
                });
            },

            subscribe : function(e, k, t, f) {
                var p = this;

                var unsubscribe = function() {
                    var l = listeners[k];

                    if (l) {
                        delete l[f];

                        if ($.isEmptyObject(l)) {
                            delete listeners[k];
                            delete tokens[k];
                            p.poll();
                        }
                    }
                };

                unsubscribe();

                $(document).bind("DOMNodeRemoved", function(r) {
                    if (r.target == e) {
                        unsubscribe();
                    }
                });

                var l = listeners[k] || { };
                l[f] = f;
                listeners[k] = l;

                if (!t || tokens[k] && tokens[k] != t) {
                    // No token supplied, or there is a listener registered
                    // for a different token. Request the current value.
                    tokens[k] = "-1";
                }
                else {
                    tokens[k] = t;
                }

                this.poll();
            },
        };

        return poller;
    };

    var poller = createPoller(document);

    function addLiveDataElements(scope) {

        $(".ld-display").each(function() {
            var t = $(this);

            poller.subscribe(this, t.data("ld-key"), t.data("ld-token"),
                function(k, v) {
                    t.html(v);
                });
        });

        $(".ld-animate").each(function() {
            var t = $(this);

            poller.subscribe(this, t.data("ld-key"), t.data("ld-token"),
                function(k, v) {
                    t
                    .stop()
                    .animate({opacity: 0.5},
                            "fast",
                            function() {
                                $(this)
                                    .html(v)
                                    .animate({opacity: 1}, "fast");
                            });
                });
         });

    }

    function addButtons(scope) {
        content = $("div#content");

        sidebarButtons = $("#sidebar .grinder-button");

        $(".grinder-button", scope).each(function() {
            if (this.id) {

                var isSidebarButton = $.inArray(this, sidebarButtons) >= 0;

                var buttonOptions;

                if (this.classList.contains("grinder-button-icon")) {
                    buttonOptions = {
                        icons: { primary: this.id }
                    };
                }
                else {
                    buttonOptions = {};
                }

                $(this).button(buttonOptions);

                var replaceContent = function(x) {
                    content.animate(
                        {opacity: 0},
                        "fast",
                        function() {
                            content.html(x);
                            addDynamicBehaviour(content[0]);
                            content.animate({opacity: 1}, "fast");
                        });
                };

                $(this).click(function() {
                    if (this.classList.contains("replace-content")) {
                        if (isSidebarButton) {
                            $(sidebarButtons).addClass("inactive");
                            $(this).removeClass("inactive");
                        }

                        $.get("/ui/content/" + this.id, replaceContent);
                    }
                    else if (this.classList.contains("post-form") &&
                             $(this.parentNode).is("form")) {

                        $.post("/ui/form/" + this.id,
                               $(this.parentNode).serialize(),
                               replaceContent);
                    }
                    else if (this.classList.contains("post-action")) {
                        $.post("/ui/action/" + this.id);
                    }
                    else {
                        console.error("Don't know how to handle click", this);
                    }
                });

            } else {
                $(this).button();
            };
        });
    }

    const TESTS_STATISTIC = 0;
    const TOTAL_TEST = "Total";
    // Half an hour at default sample interval of 1s.
    const MAX_SAMPLE_VALUES_PER_TEST = 1800;

    // Simple listener to decouple statistic collection from graph rendering.
    var notifySample = undefined;

    function cubismSampleListener(scope) {

        // Hold the statistics for a given test.
        //
        // The returned object has the following fields:
        //   test          - the test number, or TOTAL_TEST.
        //   metric(c,s)   - create a cubism metric for context c and
        //                   statistic s.
        //   add(t,s)      - adds a new sample s at timestamp t.
        //
        // The following are added to the metrics returned by metric():
        //   key              - a pair of the test number and statistic.
        //   test             - the test number, or TOTAL_TEST.
        //   description      - the test description.
        var createStatisticsHolder = function(test, description) {

            // We may want to replace this with a binary tree.
            // For now we just have an array in timestamp order.
            // Each element is an array pair of timestamp and statistic.
            // We assume that we're called with increasing timestamps.
            var stats = [];

            var average = function(ss, s) {
                // Cubism uses NaN to indicate "no value".
                var total =
                    ss.reduce(function(x, y) {
                        // If tests = 0, there is no value.
                        var v = y[TESTS_STATISTIC] ? y[s] : NaN;

                        if (isNaN(v)) { return x; }

                        if (isNaN(x)) { return v; }

                        return x + v;
                    }, NaN);

                return total / ss.length;
            };

            var metric_fn = function(statistic) {
                return function(start, stop, step, callback) {
                    var values = [];

                    start = +start; // Date -> timestamp.

                    // A cursor that iterators backwards through the stats.
                    // Returns all values within a time range.
                    var previousRange = function() {
                        var d = stats.length - 1;

                        return function(s, e) {
                            var x = stats[d];
                            var result = [];

                            while (x && x[0] >= s) {
                                d -= 1;
                                if (x[0] < e) {
                                    result.push(x[1]);
                                }

                                x = stats[d];
                            }

                            return result;
                        };
                    }();

                    for (var i = +stop; i > start; i-= step) {
                        values.unshift(
                            average(previousRange(i - step, i), statistic));
                    }

                    callback(null, values);
                };
            };

            return {
                test : test,

                metric : function(context, s) {
                    if (!this._metric ||
                        this._metric.context !== context ||
                        this._statistic !== s) {

                        this._metric = context.metric(metric_fn(s));
                        this._metric.key = [test, s];
                        this._metric.test = test;
                        this._metric.description = description;
                        this._statistic = s;
                    }

                    return this._metric;
                },

                add : function(timestamp, sample) {
                    stats.push([timestamp, sample]);

                    // If we use a more advanced structure, we might
                    // also limit based on time.
                    if (stats.length > MAX_SAMPLE_VALUES_PER_TEST * 1.1) {
                        stats = stats.slice(-MAX_SAMPLE_VALUES_PER_TEST);
                    }
                }
            };
        }

        var statisticsHolders = [];

        poller.subscribe(scope, "sample", undefined, function(k, v) {
            var existingByTest = {};

            $(statisticsHolders).each(function() {
                existingByTest[this.test] = this;
            });

            statisticsHolders = $.map(v.tests, function(t) {
                var holder =
                    existingByTest[t.test] ||
                    createStatisticsHolder(t.test, t.description);

                holder.add(v.timestamp, t.statistics);

                return holder;
            });

            var totalHolder =
                existingByTest[TOTAL_TEST] ||
                createStatisticsHolder(TOTAL_TEST, null);

            totalHolder.add(v.timestamp, v.totals);

            statisticsHolders.push(totalHolder);

            if (notifySample) {
                notifySample(statisticsHolders);
            };
        });
    }

    const CUBISM_STEP_MILLISECONDS = 2000;
    var selectedStatistic = TESTS_STATISTIC;

    function cubismCharts(scope) {
        var cubismDiv = $("#cubism");

        if (!cubismDiv.length) {
            return;
        }

        var context = cubism.context()
            .step(CUBISM_STEP_MILLISECONDS)
            .serverDelay(0)
            .clientDelay(0)
            .size(cubismDiv.width());

        $(window).resize(function(e) {
            $(this).unbind(e);
            cubismDiv.empty();
            context.stop();
            cubismCharts(scope);
        });

        // Maybe there's a neater way to do this with d3?
        $(document).bind("DOMNodeRemoved", function(e) {
            if (e.target == scope) {
                context.stop();
            }
        });

        context.on("focus", function(i) {
            d3.selectAll(".value")
              .style("right",
                      i == null ? null : context.size() - i + "px");
        });

        d3.select(scope)
            .select("#cubism")
            .selectAll(".axis")
            .data(["top", "bottom"])
            .enter().append("div")
            .attr("class", function(d) { return d + " axis"; })
            .each(function(d) {
                d3.select(this).call(
                        context.axis()
                        .orient(d)
                        .ticks(context.size() / 100)); });

        d3.select("#cubism")
            .append("div")
            .attr("class", "rule")
            .call(context.rule());

        $(document).bind("DOMNodeRemoved", function(e) {
            if (e.target == scope) {
                notifySample = undefined;
            }
        });

        // A function that updates the bound d3 data.
        var newData = function(statisticsHolders) {

            var selection = d3.select("#cubism").selectAll(".horizon");

            var metrics = $.map(statisticsHolders, function(h) {
                return h.metric(context, selectedStatistic);
            });

            // Bind tests to nodes.
            var binding = selection
                .data(function() { return metrics; },
                      function(metric) { return metric.key; });

            // Handle new nodes.
            var newTitle = binding.enter().insert("div", ".bottom")
                .attr("class", "horizon")
                .call(context.horizon()
                        .format(d3.format(",.3r"))
                        .colors(["#225EA8",
                                 "#41B6C4",
                                 "#A1DAB4",
                                 "#FFFFCC",
                                 "#FECC5C",
                                 "#FD8D3C",
                                 "#F03B20",
                                 "#BD0026"]))
                .select(".title");

            newTitle
                .html(function(m) {
                   var d = "<span class='test'>" + m.test + "</span>";

                   if (m.description) {
                     d += "<span class='description'>"
                         + m.description + "</span>";
                   }

                   return d;
                })
                .on("mouseover", function(d) {
                    d3.select(this).classed("active", true);
                })
                .on("mouseout", function(d) {
                    d3.select(this).classed("active", false);
                });

            binding.exit().remove();

            binding.sort(function(a, b) {
                if (a.test === TOTAL_TEST) {
                    return b.test === TOTAL_TEST ? 0 : 1;
                }
                else if (b.test === TOTAL_TEST) {
                    return -1;
                }

                return d3.ascending(a.test, b.test);
            });
        };

        $("select[name=chart-statistic]").val(selectedStatistic);

        $("select[name=chart-statistic]").change(function() {
            selectedStatistic = this.value;
        });

        notifySample = newData;
    }

    function addDataPanels(scope) {
        var data_state = null;
        var process_threads = null;

        poller.subscribe(scope, "sample", undefined, function(k, v) {
            var s = v.status;
            var d = $("#data-summary");

            // TODO: Requires translation.
            d.html(s.description);

            if (s.state != data_state) {
                if (s.state === "Stopped") {
                    d.parent().stop().animate({opacity: 0}, "slow");
                }
                else {
                    d.parent().stop().animate({opacity: 0.9}, "fast");
                }

                data_state = s.state;
            }
        });

        poller.subscribe(scope, "threads", undefined, function(k, v) {
            var p = $("#process-summary");

            // TODO: Requires translation.
            p.html("<span>a:" + v.agents + " </span>" +
                   "<span>w:" + v.workers + " </span>" +
                   "<span>t:" + v.threads + " </span>");

            if (v.threads != process_threads) {
                if (v.threads === 0) {
                    p.parent().stop().animate({opacity: 0}, "slow");
                }
                else {
                    p.parent().stop().animate({opacity: 0.9}, "fast");
                }

                process_threads = v.threads;
            }
        });
    }

    cubismSampleListener(document);

    function addDynamicBehaviour(scope) {
        addButtons(scope);
        addChangeDetection(scope);
        addLiveDataElements(scope);
        cubismCharts(scope);
    }

    addDataPanels(document);
    addDynamicBehaviour(document);
});
