
(function (global) {
    "use strict";

    var hasOwnProperty = global.Object.prototype.hasOwnProperty;


    var copyProperties = function (toObj, fromObj) {
        var propertyName;
        for (propertyName in fromObj) {
            if (hasOwnProperty.call(fromObj, propertyName)) {
                toObj[propertyName] = fromObj[propertyName];
            }
        }
    };


    var cloneObject = function (obj) {
        var newObj = {};
        copyProperties(newObj, obj);
        return newObj;
    };

    var zeroChars = ["0", "00"];

    var prependZeroChars = function (num, expectedLength) {
        expectedLength = expectedLength || 2;
        var numStr = "" + num,
            count = expectedLength - numStr.length;
        if (count > 0) {
            return zeroChars[count - 1] + numStr;
        }
        return numStr;
    };

    var formatDate = function (timestamp) {
        if (timestamp === 0) {
            return "None";
        }
        var date = new Date(timestamp);
        return [
            date.getFullYear(), "-",
            prependZeroChars(date.getMonth() + 1), "-",
            prependZeroChars(date.getDate()), " ",
            prependZeroChars(date.getHours()), ":",
            prependZeroChars(date.getMinutes()), ":",
            prependZeroChars(date.getSeconds())
        ].join("");
    };

    var Url = {
        _params: null,

        hasParam: function (name) {
            var params = this.getParams()
            return name in params;
        },

        getParam: function (name) {
            if (!this.hasParam(name)) {
                throw "Cannot find URL parameter: " + name;
            }
            return this.getParams()[name];
        },

        getParams: function () {
            var params = this._params;
            if (!params) {
                params = this._params = this._parseParams();
            }
            return params;
        },

        _parseParams: function () {
            var search = global.location.search.substring(1),
                pairs = search.split("&"),
                i = pairs.length,
                pair,
                map = {};
            while (i--) {
                pair = pairs[i].split("=");
                map[pair[0]] = pair[1];
            }
            return map;
        }
    };


    /**
     * Provides paging functionality.
     *
     * @class Pager
     * @constructor
     * @param {string} baseUrl
     * @param {Object} params a map of URL params
     */
    var Pager = function (baseUrl, params) {
        if (params) {
            this.params = params;
        }
        this.baseUrl = baseUrl;
        this._onsuccess = $.proxy(this._onsuccess, this);
    };

    Pager.prototype = {
        /**
         * Start index.
         *
         * @private
         * @type {number}
         */
        _start: 0,

        /**
         * Maximum number of records which the result can consist of.
         *
         * @private
         * @type {number}
         */
        _count: 20,

        /**
         * Total number of records retrieved from response.
         *
         * @private
         * @type {number}
         */
        _totalCount: 0,

        /**
         * Keeps the search pattern so that it can be reused for subsequent 
         * AJAX requests.
         *
         * @private
         * @type {string}
         */
        _matchPattern: "",
        
        /**
         * Keeps the search pattern that was used for previous search
         * so that it can be reused for subsequent 
         * AJAX requests.
         *
         * @private
         * @type {string}
         */
        _searchedPattern: "",

        /**
         * Predefined map of URL parameters.
         *
         * @type {Object}
         */
        params: {},

        /**
         * Event handler which is invoked when data arrives.
         *
         * @private
         * @type {number}
         */
        ondataloaded: null,

        startIndex: function () {
            return this._start;
        },

        matchPattern: function () {
            return this._matchPattern;
        },
        
        searchedPattern: function () {
            return this._searchedPattern;
        },

        totalCount: function (value) {
            if (value !== undefined) {
                this._totalCount = value;
            }
            return this._totalCount;
        },

        count: function () {
            return this._count;
        },

        search: function (matchPattern) {
            matchPattern = (matchPattern === undefined) ? "" : matchPattern;
            this._matchPattern = matchPattern;
            this._searchedPattern = matchPattern;
            this.load(0);
        },

        clearSearch: function () {
            this._matchPattern = "";
            this.clearSearchedPattern();
        },
        
        clearSearchedPattern: function () {
            this._searchedPattern = "";
        },

        isOnePage: function () {
            return this.totalCount() <= this.count();
        },

        load: function (startIndex) {
            this._start = startIndex;
            var matchPattern = this._matchPattern,
                ajaxConfig,
                data = cloneObject(this.params);
            data.start = startIndex;
            data.count = this._count;
            ajaxConfig = {
                url: this.baseUrl,
                data: data,
                success: this._onsuccess
            };
            if (matchPattern) {
                ajaxConfig.data.matchPattern = matchPattern;
            }
            $.ajax(ajaxConfig);
        },

        _onsuccess: function (data) {
            if (data.totalCount) {
                this.totalCount(data.totalCount);
            }
            this.ondataloaded && this.ondataloaded(data);
        },

        next: function () {
            var start = this._start + this._count;
            if (start >= this._totalCount) {
                return;
            }
            this.load(start);
        },

        prev: function () {
            var start = this._start - this._count;
            if (start < 0) {
                return;
            }
            this.load(start);
        }
    };

//------------------------------------------------------------------------------

    var ajaxStart = function () {
        $("#loading").removeClass("hidden");
    };

    var ajaxStop = function () {
        $("#loading").addClass("hidden");
    };

    var printEnvironment = function () {
        $.get("deployment-environment", function (data) {
            $("#env_info").text(data);
        });
    };

    $(document)
        .ajaxStart(ajaxStart)
        .ajaxStop(ajaxStop);

    global.Pager = Pager;
    global.Url = Url;
    global.formatDate = formatDate; 

    printEnvironment();

}(this));
