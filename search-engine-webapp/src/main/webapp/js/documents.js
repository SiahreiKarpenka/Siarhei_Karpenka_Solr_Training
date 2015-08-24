
(function (global) {

    "use strict";

    var UrlParam = {
        PACKAGE_ID: "package-id",
        PROCESS_ID: "process-id",
        STATE: "state",
        TOTAL_COUNT: "total-count",
        RECORDS_TYPE: "records-type"
    };

    var pager;

    var _templateIdMap = {};

    var cloneTemplate = function (templateId) {
        var template = _templateIdMap[templateId],
            wrapper;
        if (!template) {
            wrapper = document.getElementById(templateId);
            if (!wrapper) {
                throw "Cannot find template with ID: " + template;
            }
            template = wrapper.children[0];
        }
        return $(template.cloneNode(true));
    };

    var extractId = function (uri) {
        var indexOfLastSlash = uri.lastIndexOf("/");
        return uri.substring(indexOfLastSlash + 1);
    };

    var render = function (data) {
        function createRecord(index, item) {
            var messages = item.messages,
                messagesLength = messages.length,
                i, templateMsgCol, template;
            template = cloneTemplate("tmpl_doc_item");
            template.find(".doc-name-col")
                    .attr("title", item.originalFileName)
                    .text(item.originalFileName);
            template.find(".doc-uri-col")
                    .attr("title", item.uri)
                    .text("..." + extractId(item.uri));
            template.appendTo("#content_body");
            templateMsgCol = template.find(".doc-msg-col");
            for (i= 0; i < messagesLength; i++) {
                $('<div class="doc-err-msg"></div>')
                        .text(messages[i].message)
                        .appendTo(templateMsgCol);
            }
        }
        $("#content_body").empty();
        $.each(data, createRecord);
    };

    var updatePageLabel = function () {
        var totalCount = pager.totalCount(),
            start = pager.startIndex(),
            end = start + pager.count();
        if (start === 0) start++;
        if (end > totalCount) end = totalCount;
        $(".paging-label").text([
            "Displayed ", start, " - ", end, " of ", totalCount
        ].join(""));
    };

    var onDataLoaded = function (data) {
        updatePageLabel();
        render(data);
    };

    var initPage = function () {
        var pagerParams,
            recordsType = Url.getParam(UrlParam.RECORDS_TYPE),
            totalCount = Url.getParam(UrlParam.TOTAL_COUNT);
        pagerParams = {
            "packageId": Url.getParam(UrlParam.PACKAGE_ID),
            "state": Url.getParam(UrlParam.STATE),
            "process": Url.getParam(UrlParam.PROCESS_ID)
        };
        if (recordsType === 'warning') {
            pager = new Pager("documentsWithWarnings", pagerParams);
            $(".title-bar").find("h1").text("Documents With Warnings");
        } else {
            pager = new Pager("documentsWithErrors", pagerParams);
            $(".title-bar").find("h1").text("Documents With Errors");
        }
        
        pager.totalCount(totalCount);
        pager.ondataloaded = onDataLoaded;
    };

    initPage();

    var Page = {

        _context: {},

        getDocuments: function () {
            $.ajax({
                url: "documentsWithErrors",
                data: {
                    "packageId": Url.getParam("package-id"),
                    "process": Url.getParam("process-id"),
                    "state": Url.getParam("state")
                },
                success: $.proxy(this._onDocumentsLoaded, this)
            });
        },

        _onDocumentsLoaded: function (data) {
            
        },

        _initContext: function () {
            var context = this._context;
            context.packageId = Url.getParam("package-id");
            context.state = Url.getParam("state");
            context.process = Url.getParam("process-id");
        },

        _onLinkClicked: function (ev) {
            var context = this._context,
                link = $(ev.currentTarget),
                url = [
                    "error_list.html?",
                    "package-id=", context.packageId,
                    "&process-id=", context.process,
                    "&state=", context.state,
                    "&doc-uri=", link.attr("data-doc-uri")
                ].join("");
            window.open(url, "_blank");
        },

        init: function () {
            this._initContext();
            //Page.getDocuments();
            $("#package_name").text(this._context.packageId);
            $("#process_id").text(this._context.process);
            $("#content").on("click", ".show-errors", $.proxy(this._onLinkClicked, this));
        }
    };

    $("#page_prev").click(function (ev) {
        pager.prev();
    });

    $("#page_next").click(function (ev) {
        pager.next();
    });

    pager.load(0);

    Page.init();

}(this));





























