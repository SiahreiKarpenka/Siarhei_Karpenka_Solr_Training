

(function (global) {
    "use strict";

    var formatDate = global.formatDate;

    var pager = new Pager("packages/overview");

    var dataCache = null;

    pager.ondataloaded = function (data) {
        dataCache = data;
        renderData(data);
    };


    var renderData = function (data) {
        TableRenderer.render(data);
        updatePagingLabel(data);
    };


    var updateData = function () {
        pager.load(0);
    };

    var updatePagingLabel = function (data) {
        var totalCount = data.totalCount,
            start = pager._start,
            end = start + pager._count;
        if (start === 0) {
            start++;
        }
        if (end > totalCount) {
            end = totalCount;
        }
        $(".paging-label").text([
            "Displayed ", start, " - ", end, " of ", totalCount
        ].join(""));
    };

    var TableRenderer = {
        table: $("#overview_table"),
        
        render: function (data) {
            this.renderTotal(data);
            this.table.find("tbody").empty();
            var tbody = this.table[0].getElementsByTagName("tbody")[0],
            	savedStateTotalReports = data["savedStateTotalReports"],
                length,
                i;
	        if (!savedStateTotalReports) {
	            // TODO raise exception;
	            return;
	        } 
    		length = savedStateTotalReports.length;
            for (i = 0; i < length; i++) {
            	this.renderTotalRow(tbody, savedStateTotalReports[i], "savedStateTotalReports");
            }
        },

        renderTotal: function (data) {
            this.table.find(".total-row, .total-row-prev").remove();
            var currentStateTotalReport = data["currentStateTotalReport"],
            	parent = this.table.find("thead");
            if (!currentStateTotalReport) {
                // TODO raise exception;
                return;
            } 
            this.renderTotalRow(parent, currentStateTotalReport, "currentStateTotalReport");
        },

        renderTotalRow: function (parent, totalReport, propertyName) {
            var processReport,
                totalRow,
                totalRowClassName,
                totalCell,
                isSavedState = function () {
                    return propertyName === "savedStateTotalReports";
                };

            if (!totalReport) {
                // TODO raise exception;
                return;
            }

            totalRow = $('<tr>');
            totalRowClassName = isSavedState() ? "total-row-prev" : "total-row";
            totalRow.addClass(totalRowClassName);

            
            if (isSavedState()) {
            	processReport = {processesReports: totalReport.processesReport};            	
            	$('<td colspan="2">')
                .text(formatDate(totalReport.savedStateTimestamp))
                .appendTo(totalRow);
            } else {
            	totalCell = $('<td colspan="2" class="total-cell">');
            	processReport = {processesReports: totalReport};
                $('<div class="rel">')
                    .text("Current State")
                    .appendTo(totalCell);
                totalCell.appendTo(totalRow);
            }
            
           
            this.renderProcessReport(totalRow, processReport, "content-acquisition-s2");
            this.renderProcessReport(totalRow, processReport, "content-validation-condor");
            this.renderProcessReport(totalRow, processReport, "condor-export-production");
            totalRow.appendTo(parent);
        },

        renderProcessReport: function (tr, packageReport, processName) {
            var processReports = packageReport.processesReports,
                processReport = this.getProcessReportByName(
                        processReports, processName);

            if (!processReport) {
                throw "Cannot find process " + processName;
            }

            /*if (processName === "content-validation-condor") {
                okDocs = processReport.totalDocs;
            } else {
                okDocs = processReport.totalDocs - processReport.errorDocs;
            }*/

            $('<td>')
                    .text(processReport.totalDocs)
                    .appendTo(tr);

            $('<td>')
                    .text(processReport.errorDocs)
                    .appendTo(tr);
        },

        getProcessReportByName: function (processReports, processName) {
            var i = processReports.length,
                processReport;
            while (i--) {
                processReport = processReports[i];
                if (processReport.processName === processName) {
                    return processReport;
                }
            }
        }
    };


    $("#page_prev").click(function () {
        pager.prev();
    });


    $("#page_next").click(function () {
        pager.next();
    });

    updateData();

}(this));