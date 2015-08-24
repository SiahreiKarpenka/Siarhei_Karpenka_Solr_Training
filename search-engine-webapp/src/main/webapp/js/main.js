

(function (global) {

"use strict";

var getBaseName = function (filename) {
    var lastDotIndex = filename.lastIndexOf(".");
    if (lastDotIndex === -1) {
        lastDotIndex = filename.length;
    }
    return filename.substring(0, lastDotIndex);
};

var formatDate = global.formatDate;

//------------------------------------------------------------------------------


var Page = {

    _showSavedState: false,
    _cache: null,
    _cipTable: $("#cipTable"),
    _pager: new Pager("packages/acquired"),

    update: function () {
        this._pager.ondataloaded = $.proxy(this._onDataLoaded, this);
        this._pager.clearSearch();
        this._pager.load(0);
    },

    switchFilter: function (url) {
        this._pager.baseUrl = url;
        this.update();
    },

    search: function (matchPattern) {
        this._pager.baseUrl = "packages/all";
        this._pager.search(matchPattern);
    },

    nextPage: function () {
        this._pager.next();
    },

    prevPage: function () {
        this._pager.prev();
    },

    clearSearch: function () {
        var pager = this._pager,
            searchedPattern = $.trim(pager.searchedPattern());
        if (searchedPattern !== "") {
            this.search("");
        }
    },

    clearSearchedPattern: function () {
        this._pager.clearSearchedPattern();
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

    _updatePagingLabel: function (data) {
        var totalCount = data.totalCount,
            pager = this._pager,
            startCount = pager._start,
            endCount = startCount + pager._count,
            message;
        if (!totalCount) {
            $('.paging-label').text('Nothing to display');
            return;
        }
        if (startCount === 0) {
            startCount++;
        }
        if (endCount > totalCount) {
            endCount = totalCount;
        }
        message = 'Displayed ';
        message += startCount;
        message += ' - ';
        message += endCount;
        message += ' of ';
        message += totalCount;
        $(".paging-label").text(message);
    },

    _onStateSaved: function () {
    	if(this._pager.searchedPattern() === "") {
    		this.update();
    	} else {
    		$("#btn_search").click();
    	}
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

    _filter: function (data) {
        var condition;
        if (!this._showSavedState) {
            condition = function (item) {
                return !item.state || item.state === "CURRENT";
            };
            return $.grep(data, condition);
        }
        return data;
    },

    _repaint: function (data) {
        var cipTable = this._cipTable,
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

    saveState: function () {
        $.ajax({
            url: "saveState",
            type: "put",
            success: $.proxy(this._onStateSaved, this)
        });
    },

    toggleSavedState: function () {
        this._showSavedState = !this._showSavedState;
        this._repaint(this._filter(this._cache.packagesReports));
    },

    updateSavedState: function (showSavedState) {
        this._showSavedState = showSavedState;
        if (showSavedState) {
            this.saveState();
        } else {
            this._repaint(this._filter(this._cache.packagesReports));
        }
    },

    init: function () {
        this._cipTable.on("click", "tbody .clickable", this._onCellClicked);
        this.update();
    }
};


var TableRowRenderer = {

    _empty: {
        errorDocs: 0,
        totalDocs: 0,
        processName: "",
        errorBinaryObjects: 0,
        totalBinaryObjects: 0
    },

    _empty_arr: null,

    _getEmptyArray: function () {
        var empty_arr = this._empty_arr,
            empty,
            i;
        if (!empty_arr) {
            empty = this._empty;
            empty_arr = [];
            i = 4;
            while (i--) {
                empty_arr[i] = empty;
            }
            this._empty_arr = empty_arr;
        }
        return empty_arr;
    },

    createRow: function (data) {
        var row = $("<tr>"),
            packageName = data.packageName,
            dataAcquisition,
            dataValidationS2,
            dataConversionS2topci,
            text,
            doctype = data.doctype || "",
            contentSet = data.contentSet || "",
            origin = data.origin,
            rerunButton,
            packageNameCell;
        
        $("<td></td>")
                .attr("title", origin)
                .text(origin)
                .appendTo(row);

        $("<td></td>")
                .attr("title", doctype)
                .text(doctype)
                .appendTo(row);

        $("<td></td>")
                .attr("title", contentSet)
                .text(contentSet)
                .appendTo(row);

        if (data.state === "CURRENT" || data.state === null) {
            text = "";
            text += packageName;
            text += "";
        } else {
            text = "Saved State (";
            text += packageName;
            text += ")";
        }

        packageNameCell = $("<td>");

        $('<span>')
                .text(text)
                .attr("title", text)
                .appendTo(packageNameCell);

        //Rerun button is commented out until the backaend functionality is stable
        if (data.state === "CURRENT") {
            rerunButton = $('<a href="#" class="rerun-button">Rerun</a>');
            rerunButton.attr("data-package-name", packageName);
            rerunButton.appendTo(packageNameCell);
        }
        packageNameCell.appendTo(row);

        row.append("<td>" + formatDate(data.acquisitionTimestamp) + "</td>");

        dataAcquisition = this._find(data, "content-acquisition-s2");
        row.append("<td>" + dataAcquisition.totalDocs + "</td>");
        row.append("<td>" + dataAcquisition.totalBinaryObjects + "</td>");

        /*
        if (!dataAcquisition.errorBinaryObjects) {
            row.append("<td>" + dataAcquisition.errorBinaryObjects + "</td>");
        } else {
            $("<td></td>")
                        .addClass("clickable")
                        .text(dataAcquisition.errorBinaryObjects)
                        .attr("title", "Show error documents")
                        .attr("data-package-id", data.packageName)
                        .attr("data-process-id", dataAcquisition.processName)
                        .attr("data-state", data.state)
                        .attr("data-errors", dataAcquisition.errorBinaryObjects)
                        .appendTo(row);
        }
        row.append("<td>0</td>");
        */
/*
        if (dataAcquisition.totalDocs === 0) {
            row.append("<td>None</td>");
        } else if (dataAcquisition.errorDocs === 0) {
            row.append("<td>OK</td>");
        } else {
            $("<td></td>")
                .addClass("error clickable")
                .text("ERROR")
                .attr("title", "Show error documents")
                .attr("data-package-id", data.packageName)
                .attr("data-process-id", dataAcquisition.processName)
                .attr("data-state", data.state)
                .attr("data-errors", dataAcquisition.errorDocs)
                .appendTo(row);
        }*/
        this._createCells(data, row, 0);
        return row;
    },

    _find: function (data, processId) {
        var reports = data.processesReports,
            report,
            i;
        if (!reports) {
            return this._empty;
        }
        i = reports.length;
        while (i--) {
            report = reports[i];
            if (report.processName === processId) {
                return report;
            }
        }
        return this._empty;
    },

    _createCells: function (data, row, startIndex) {
        var reports = data.processesReports || this._getEmptyArray(),
            report,
            reportsLength = reports.length,
            i = startIndex || 0,
            okDocs;
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
                $("<td></td>")
                        .addClass("warning clickable")
                        .text(okDocs)
                        .attr("title", "Show warning documents")
                        .attr("data-package-id", data.packageName)
                        .attr("data-process-id", report.processName)
                        .attr("data-state", data.state)
                        .attr("data-errors", report.warningDocs)
                        .appendTo(row);
            }
            
            if (!report.errorDocs) {
                row.append("<td>" + report.errorDocs + "</td>");
            } else {
                $("<td></td>")
                        .addClass("error clickable")
                        .text(report.errorDocs)
                        .attr("title", "Show error documents")
                        .attr("data-package-id", data.packageName)
                        .attr("data-process-id", report.processName)
                        .attr("data-state", data.state)
                        .attr("data-errors", report.errorDocs)
                        .appendTo(row);
            }
        }
    }
};


//------------------------------------------------------------------------------

Page.init();

$("#save_state_btn").click(function (ev) {
    Page.saveState();
});


$("#toggle_saved_state_btn").click(function (ev) {
    $(this).toggleClass("active");
    Page.toggleSavedState();
});

var filterUrls = [
    "packages/acquired",
    "packages/error",
    "packages/psa",
    "packages/acquired?sort=desc",
    "packages/mercury",
    "packages/all",
    "packages/acquired/not"
];

$(window)
    .click(function () {
        $("#package_filter_switcher").removeClass("open");
    })
    .keydown(function (ev) {
        var KEY_ESCAPE = 27;
        if (ev.keyCode === KEY_ESCAPE) {
            $("#package_filter_switcher").removeClass("open");
        }
    });

$("#package_filter_switcher")
        .on("click", function (ev) {
            $("#package_filter_switcher").toggleClass("open");
            ev.preventDefault();
            ev.stopPropagation();
        })
        .on("click", ".ddl-item", function (ev) {
            var node = $(this),
                selectedIndex = node.attr("data-index");
            $("#package_filter_switcher .ddl-header-text").text(node.text());
            Page.switchFilter(filterUrls[selectedIndex]);
            $("#match_pattern").val("");
            $("#clear_search").addClass("hidden");
        });

$("#btn_search").click(function () {
    var matchPattern = $("#match_pattern").val();
    var matchPatternValue = $.trim(matchPattern);
    var filter = $("#item_packages_all");
    $("#package_filter_switcher .ddl-header-text").text(filter.text());
    if (matchPatternValue) {
        $("#clear_search").removeClass("hidden");
    }
    Page.search(matchPatternValue);
});

$("#match_pattern").keydown(function (ev) {
    var KEY_ENTER = 13,
        KEY_ESCAPE = 27,
        keyCode = ev.keyCode;

    if (keyCode === KEY_ENTER) {
        $("#btn_search").click();
    } else if (keyCode === KEY_ESCAPE) {
        $("#clear_search").click();
    } else {
        Page.clearSearchedPattern();
    }
});

$("#clear_search").click(function () {
    $("#clear_search").addClass("hidden");
    $("#match_pattern").val("");
    Page.clearSearch();
});

$("#page_prev").click(function (ev) {
    Page.prevPage();
});

$("#page_next").click(function (ev) {
    Page.nextPage();
});


var rerunPackageName;

$("#cipTable").on("click", ".rerun-button", function (ev) {
    rerunPackageName = $(this).attr("data-package-name");
    $("#rerun_popup_packagename").text(rerunPackageName);
    $("#rerun_popup").show();
    ev.preventDefault();
});




$("#rerun_popup_ok").click(function (ev) {
    var processName = $("#rerun_process").val(),
        packageName = rerunPackageName,
        url,

        hidePopup = function () {
            $("#rerun_popup").hide();
        },

        showErrorMessage = function (message) {
            $("#error_message").text(message);
        },

        createUrl = function (processName, packageName) {
            var url = "rerun/";
            url += processName;
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

    if (!processName || !packageName) {
        showErrorMessage("Process is not specified");
        return;
    }

    url = createUrl(processName, packageName);
    
    $.ajax({
        url: url,
        success: success,
        error: error
    });
});

$("#rerun_popup_cancel").click(function (ev) {
    $("#rerun_process").val('');
    $("#error_message").text('');
    $("#rerun_popup").hide();
});


}(this));