(function (global) {
    "use strict";

    var dataCache = null;

    var emptyProcess = {
        processName: "",
        statSpd: 0,
        statDpm: 0
    };

    var FieldUnitMap = {
        "statDpm": "doc/min",
        "statSpd": "s/doc",
        "runningPackages": "package(s)",
        "runningDocuments": "document(s)"
    };

    var printData = function (processArray) {
        var tableBodyElement = $("#performance_table tbody"),
            tableRow;

        tableRow = createTableRowWithHeader("Average number of documents per minute");
        appendStatToTableRow(tableRow, processArray, "statDpm");
        tableRow.appendTo(tableBodyElement);

        tableRow = createTableRowWithHeader("Average time in seconds per document");
        appendStatToTableRow(tableRow, processArray, "statSpd");
        tableRow.appendTo(tableBodyElement);

        tableRow = createTableRowWithHeader("Queue and work in progress");
        appendStatusToTableRow(tableRow, processArray, "");
        tableRow.appendTo(tableBodyElement);
    };

    var createTableRowWithHeader = function (headerText) {
        var tableRow = $("<tr>");
        $("<td>")
            .text(headerText)
            .appendTo(tableRow);
        return tableRow;
    };

    var appendStatToTableRow = function (tableRow, processArray,
            propertyName) {
        var length = processArray.length,
            i, value;
        for (i = 0; i < length; i++) {
            value = round(processArray[i][propertyName]);
            value += " ";
            value += FieldUnitMap[propertyName];
            $("<td>")
                .text(value)
                .appendTo(tableRow);
        }
        return tableRow;
    };

    var appendStatusToTableRow = function (tableRow, processArray) {
        var length = processArray.length,
            i,
            process,
            runningDocumentsValue,
            runningPackagesValue,
            value;
        for (i = 0; i < length; i++) {
            process = processArray[i];
            runningDocumentsValue = process.runningDocuments;
            runningPackagesValue = process.runningPackages;
            if (!runningPackagesValue && !runningDocumentsValue) {
                value = "idle";
            } else if (process.processName === "content-acquisition-s2") {
                /*
                value = runningPackagesValue;
                value += " ";
                value += FieldUnitMap.runningPackages;
                value += ", ";
                */
                value = runningDocumentsValue;
                value += " ";
                value += FieldUnitMap.runningDocuments;
            } else {
                value = runningDocumentsValue;
                value += " ";
                value += FieldUnitMap.runningDocuments;
            }
            $("<td>")
                .text(value)
                .appendTo(tableRow);
        }
        return tableRow;
    };

    var round = function (value) {
        return value.toFixed(2);
    };


    $.ajax({
        url: "performance",
        dataType: "JSON",
        success: function (data) {
            dataCache = data.metrics;
            while (data.metrics.length < 5) {
                data.metrics[data.metrics.length] = emptyProcess;
            }
            printData(data.metrics);
            
            $("#last-updated-time").text(formatDate(data.lastUpdated));
        }
    });
    

    var inRange = function (number, start, end) {
        return number >= start && number <= end;
    };

    $("#calc_docNum").keypress(function (ev) {
        var charCode = ev.which;
        console.log(charCode);
        if (!inRange(charCode, 48, 57)) {
            ev.preventDefault();
        }
    });

    $("#calc_calcBtn").click(function (ev) {
        var documentCount = $.trim($("#calc_docNum").val()),
            processArray = dataCache,
            i = processArray.length,
            estimatedTimeSeconds = 0;
        if (!processArray) {
            return;
        }
        while (i--) {
            estimatedTimeSeconds += processArray[i].statSpd * documentCount;
        }
        printEstimatedTime({
            documentCount: documentCount,
            estimatedTimeSeconds: estimatedTimeSeconds
        });
    });

    var printEstimatedTime = function (data) {
        var seconds = data.estimatedTimeSeconds,
            minutes = seconds / 60,
            hours,
            formattedTime = "";
        if (minutes < 1) {
            seconds = Math.round(seconds);
            formattedTime += seconds;
            formattedTime += appendCharS(seconds, " second");
        } else if (minutes < 60) {
            minutes = Math.round(minutes);
            formattedTime += minutes;
            formattedTime += appendCharS(minutes, " minute");
        } else {
            hours = Math.floor(minutes / 60);
            minutes = Math.floor(minutes - (hours * 60));
            formattedTime = hours;
            formattedTime += appendCharS(hours, " hour");
            if (minutes >= 1) {
                formattedTime += " ";
                formattedTime += minutes;
                formattedTime += appendCharS(minutes, " minute");
            }
        }
        $("#calc_resultDocNum").text(data.documentCount);
        $("#calc_resultTime").text(formattedTime);
        $("#calc_time").show();
    };

    var appendCharS = function (number, unit) {
        return (number === 1) ? unit : unit + "s";
    };

}(this));