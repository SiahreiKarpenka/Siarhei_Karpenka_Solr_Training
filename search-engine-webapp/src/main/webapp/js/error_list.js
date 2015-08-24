

(function (global) {
    "use strict";

    $.ajax({
        url: "documentsErrorsMessages",
        data: {
            "packageId": Url.getParam("package-id"),
            "process": Url.getParam("process-id"),
            "state": Url.getParam("state"),
            "docURI": Url.getParam("doc-uri")
            
        },
        success: function (data) {
            var tbody = $("#error_list tbody");
            $.each(data, function (index, item) {
                var td = $("<td>")
                    .attr("title", item.process)
                    .text(item.process)
                $("<tr>")
                    .append(td)
                    .append('<td class="message-cell">' + item.message + "</td>")
                    .append("<td>ERROR</td>")
                    .appendTo(tbody);
            });
        }
    });
    
    $("#package_name").text(Url.getParam("package-id"));
    $("#doc_uri").text(Url.getParam("doc-uri"));

}(this));