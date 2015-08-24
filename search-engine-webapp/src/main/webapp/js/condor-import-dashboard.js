
(function(global) {
    "use strict";
	var Page = {
		_cache: null,
		_pager: new Pager("packages/condorImportOverview"),
		_overviewTable: $("#overview_table"),
		
	    update: function () {
	        this._pager.ondataloaded = $.proxy(this._onDataLoaded, this);
	        this._pager.load(0);
	    },
	    
	    renderData: function(data) {
	        TableRenderer.render(data);
	        this.updatePagingLabel(data);
	    },
	
	    updateData : function() {
	    	this._pager.load(0);
	    },

	    nextPage: function () {
	        this._pager.next();
	    },

	    prevPage: function () {
	        this._pager.prev();
	    },
	    
	    init: function() {
	    	this._overviewTable.on("click", "tbody .clickable", this._onCellClicked);
	    	this._pager.ondataloaded = function(data) {
	            Page.renderData(data);
	        };
	    },

	    _onDataLoaded: function (data) {
	        this._cache = data;
	        this._updatePagingLabel(data);
	        if (data && data.packagesReports) {
	            this._repaint(this._filter(data.packagesReports));
	        } else {
	            this._repaint([]);
	        }
	        if (this._pager.isOnePage()) {
	            // disable buttons
	        } else {
	            // disable buttons
	        }
	    },
	
	    updatePagingLabel : function(data) {
	        var totalCount = data.totalCount, start = this._pager._start, end = start
	                + this._pager._count;
	        if (!totalCount) {
	            $('.paging-label').text('Nothing to display');
	            return;
	        }
	        if (start === 0) {
	            start++;
	        }
	        if (end > totalCount) {
	            end = totalCount;
	        }
	        $(".paging-label").text(
	                [ "Displayed ", start, " - ", end, " of ", totalCount ]
	                        .join(""));
	    },

	    _onCellClicked: function (ev) {
	        var clickedCell = $(this),
	            cellClass = clickedCell.attr("class"),
	            recordsType = cellClass.substr(0,cellClass.indexOf(' ')),
	            url = [
	                "documents.html?",
	                "package-id=", clickedCell.attr("data-package-id"),
	                "&process-id=", clickedCell.attr("data-process-id"),
	                "&state=", clickedCell.attr("data-state"),
	                "&total-count=", clickedCell.attr("data-errors"),
	                "&records-type=", recordsType 
	            ].join("");
	        window.location.href = url;
	        //window.open(url, "cipdoclist");
	    },

	    _repaint: function (data) {
	        var cipTable = this._overviewTable,
	            tbody = cipTable.children("tbody"),
	            tr,
	            dataLength = data.length,
	            i = 0;
	        tbody.remove();
	        tbody.empty();
	        while (i < dataLength) {
	            tr = TableRowRenderer.createRow(data[i]);
	            tbody.append(tr);
	            i++;
	        }
	        cipTable.append(tbody);
	    },
	};

    var TableRenderer = {
        table : $("#overview_table"),

        render : function(data) {
            var tbody = 
                    this.table.find("tbody"),
                    packageReports = data.packagesReports,
                    length, i, tr;
            tbody.empty();
            if(data && packageReports) {
                length = packageReports.length;
                for (i = 0; i < length; i++) {
                    tr = this.createRow(packageReports[i]);
                    tbody.append(tr);
                }
            }
        },

        createRow : function(data) {
            var row = $("<tr>"), packageName = data.packageName, contentSet = data.contentSet
                    || "", packageNameCell = $("<td>"), rerunButton;

            $("<td></td>").attr("title", contentSet).text(contentSet).appendTo(
                    row);
            
            $('<span>')
            .text(packageName)
            .attr("title", packageName)
            .appendTo(packageNameCell);

            rerunButton = $('<a href="#" class="rerun-button">Rerun</a>');
            rerunButton.attr("data-package-name", packageName);
            rerunButton.appendTo(packageNameCell);
            packageNameCell.appendTo(row);

            this._createCells(data, row, 0);

            return row;
        },

        _getEmptyArray: function () {
            var empty_arr = this._empty_arr,
                empty,
                i;
            if (!empty_arr) {
                empty = this._empty;
                empty_arr = [];
                i = 6;
                while (i--) {
                    empty_arr[i] = empty;
                }
                this._empty_arr = empty_arr;
            }
            return empty_arr;
        },

        _createCells : function(data, row, startIndex) {
            var reports = data.processesReports || this._getEmptyArray(), report, reportsLength = reports.length, i = startIndex || 0, okDocs;
            for (; i < reportsLength; i++) {
                report = reports[i];
                if (report.totalDocs === 0) {
                    okDocs = 0;
                } else {
                    okDocs = report.totalDocs - report.errorDocs;
                }
                if (!report.warningDocs) {
                    row.append("<td>" + okDocs + "</td>");
                } else {
                    $("<td></td>").addClass("warning clickable").text(okDocs)
                            .attr("title", "Show warning documents").attr(
                                    "data-package-id", data.packageName).attr(
                                    "data-process-id", report.processName)
                            .attr("data-state", data.state).attr("data-errors",
                                    report.warningDocs).appendTo(row);
                }

                if (!report.errorDocs) {
                    row.append("<td>" + report.errorDocs + "</td>");
                } else {
                    $("<td></td>").addClass("error clickable").text(
                            report.errorDocs).attr("title",
                            "Show error documents").attr("data-package-id",
                            data.packageName).attr("data-process-id",
                            report.processName).attr("data-state", data.state)
                            .attr("data-errors", report.errorDocs)
                            .appendTo(row);
                }
            }
        }
    };

    $("#page_prev").click(function() {
        Page.prevPage();
    });

    $("#page_next").click(function() {
        Page.nextPage();
    });

    Page.updateData();
    Page.init();
    
    var rerunPackageName;

    $("#overview_table").on("click", ".rerun-button", function (ev) {
        rerunPackageName = $(this).attr("data-package-name");
        $("#rerun_popup_packagename").text(rerunPackageName);
        $("#rerun_popup").show();
        ev.preventDefault();
    });

    $("#rerun_popup_ok").click(function (ev) {
        var providerName = $("#rerun_provider").val(),
            packageName = rerunPackageName,
            url,

            hidePopup = function () {
                $("#rerun_popup").hide();
            },

            showErrorMessage = function (message) {
                $("#error_message").text(message);
            },

            createUrl = function (providerName, packageName) {
                var url = "rerun-condor/";
                url += providerName;
                url += "?package="
                url += packageName;
                return url;
            },

            success = function (data) {
                if (data === 0) {
                    showErrorMessage("No documents will be selected to re-run.");
                } else {
                    hidePopup();
                }
            },

            error = function (data) {
            };

        if (!providerName || !packageName) {
            showErrorMessage("Process is not specified");
            return;
        }

        url = createUrl(providerName, packageName);
        
        $.ajax({
            url: url,
            success: success,
            error: error
        });
    });

    $("#rerun_popup_cancel").click(function (ev) {
        $("#rerun_provider").val('');
        $("#error_message").text('');
        $("#rerun_popup").hide();
    });

}(this));