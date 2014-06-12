/**
 * Builds a page for configuring the values in a column for a single input type (e.g. a security or trade type).
 * The module exposes two functions, initialize() and updateConfig().
 * <p>
 * initialize() should be called once when the page first loads.
 * <p>
 * updateConfig() should be called update the current configuration from the form components.
 */
var configModule = (function () {

  /** The current configuration, updated as the user edits the form. */
  var config,
      /** ID of the column being edited. */
      columnId,
      /**
       * Flags whether we are editing config for an existing output or adding a new one. If it's an existing output
       * the input type can't be changed.
       */
      editing,
      /** URL of the REST endpoint for the column's model. */
      modelEndpointUrl;

  /**
   * Initializes the page - attaches listeners to update the model when the data changes and requests the initial model.
   *
   * @param colId the ID of the column being edited
   * @param inputType the type of input whose function configuration is being edited. Can be null if a new output
   *   is being configured and the user must select the input type.
   */
  function initialize(colId, inputType) {
    var configUrl;

    columnId = colId;
    config = {impls: {}, args: {}, inputType: inputType, outputName: ""};
    modelEndpointUrl = "/columns/" + columnId;

    editing = !!inputType;

    if (!inputType) {
      configUrl = "/columns/defaultconfig";
    } else {
      configUrl = "/columns/" + columnId + "/" + inputType;
    }
    // request the existing config (or the default if we're adding config for an output where none exists)
    $.get(configUrl, function (response, textStatus, jqXHR) {
      configReceived(JSON.parse(jqXHR.responseText));
    });

    /**
     * Initializes the config var from the server.
     * This is only done once when the page is created. After that it's updated from the form each time the
     * config is submitted to the server.
     *
     * @param configJson the config for the column and input type being edited. If the input type is unknown
     *   (i.e. the user is adding a new output) this is the system default config.
     */
    function configReceived(configJson) {
      console.log("received config");
      console.log(configJson);
      config.impls = configJson.impls;
      config.args = configJson.args;
      $.post(modelEndpointUrl, JSON.stringify(config), function (response, textStatus, jqXHR) {
        receivedModel(jqXHR.responseText);
      });
    }

    // this allows the config to be rebuilt in response to the user pressing enter in a text field
    $("form").submit(function (evt) {
      evt.preventDefault(); // don't actually submit the form to the server
      submitConfig();
    });
    $("#input-type-select, #output-name-select").change(function () {
      submitConfig();
    })
  }

  /**
   * Creates the components for a function interface whose implementation must be selected.
   * A select input is created for selecting the implementation and a text field is created for each constructor
   * argument.
   *
   * @param fn the function {name: "the display name", type: "the Java class"}
   * @param impls array of {name: "the display name", type: "the Java class"}
   * @param selImpl the selected implementation, {name: "the display name", type: "the Java class"}, possibly null
   * @param args array of {name: "the display name", value: "the value, optional", error: "error message, optional"}
   * @returns {Array} or jQuery wrapping the elements
   */
  function createInterfaceComponents(fn, impls, selImpl, args) {
    var row = $("#selector-row").clone().removeAttr("id").removeClass("template"),
        implType = selImpl ? selImpl["type"] : null,
        select = row.find("select").attr("name", fn["type"]),
        options = impls.map(function (impl) {
          return $("<option>").text(impl["name"]).attr("value", impl["type"]);
        });
    row.find("#selector-label").text(fn["name"]);
    select.empty();

    if (!selImpl) {
      select.append("<option>")
    }
    select.append(options).val(implType);
    row.find("*").removeAttr("id");

    if (impls.length == 0) {
      row.find(".error").text("No functions available");
    } else if (!implType) {
      row.find(".error").text("Select a function");
    }
    select.change(function () {
      submitConfig();
    });
    return [row].concat(createArgumentFields(implType, args));
  }

  /**
   * Creates the components for a function whose implementation type is known.
   * This can be a function interface with only one known implementation or it can be a concrete class.
   * A label is created for the implementation name and a text field is created for each constructor
   * argument.
   *
   * @param fn
   * @param impl
   * @param args
   * @returns {Array}
   */
  function createImplementationComponents(fn, impl, args) {
    var row = $("#known-type-row").clone().removeAttr("id").removeClass("template"),
        fnName = fn ? fn["name"] : impl["name"],
        implName = fn ? impl["name"] : "";

    row.find("#known-type-label1").text(fnName);
    row.find("#known-type-label2").text(implName);
    return [row].concat(createArgumentFields(impl["type"], args));
  }

  /**
   * Calls createArgumentField for each argument in the array
   *
   * @param implType the function implementation type
   * @param args the function's constructor arguments
   * @returns {Array|*} array of jQuery objects wrapping the components for entering the arguments
   */
  function createArgumentFields(implType, args) {
    return args.map(function (arg) {
      return createArgumentField(implType, arg["name"], arg["value"], arg["error"], arg["type"], arg["configs"]);
    });
  }

  /**
   * Creates form components for specifying a constructor argument.
   * If the argument type is annotated with @Config a select is created populated with names of object in the
   * config database. Otherwise a text field is created and it is assumed the object can be created by parsing
   * a string
   *
   * @param implType the function implementation type
   * @param argName the name of the argument's parameter
   * @param argValue the argument value
   * @param error the error message for the argument, null if there's no error
   * @param type the argument's type
   * @param configs array of applicable configs from the DB if the argument type has a Config annotations
   * @returns {*|jQuery} array of jQuery objects wrapping all components for entering the argument
   */
  function createArgumentField(implType, argName, argValue, error, type, configs) {
    var row = $("#argument-row").clone(),
        fieldName = implType + "/" + argName,
        argumentSelect = row.find("#argument-select"),
        argumentInput = row.find("#argument-input"),
        options;
    if (configs) {
      options = $.map(configs, function (configName) {
        return $("<option>", {value: configName}).text(configName);
      });
      if (!argValue) {
        options.unshift($("<option>"));
      }
      argumentSelect.empty().append(options);
      if (argValue) {
        argumentSelect.val(argValue);
      }
      argumentSelect.attr("value", (argValue || "")).attr("name", fieldName);
      argumentInput.hide();
      argumentSelect.show();
      argumentSelect.change(function () {
        submitConfig();
      });
    } else {
      argumentInput.attr("value", (argValue || "")).attr("name", fieldName);
      argumentInput.show();
      argumentSelect.hide();
    }
    row.find("#argument-label").text(argName);
    row.find("#argument-type").text(type);
    row.find(".error").text(error || "");
    row.find("*").removeAttr("id");
    return row.removeAttr("id").removeClass("template");
  }

  /**
   * Callback invoked when a new function model is received.
   *
   * @param responseBody JSON that represents the parts of the function model that need to be exposed to the user
   *   so they can enter configuration
   */
  function receivedModel(responseBody) {
    var json = JSON.parse(responseBody);
    console.log("received model");
    console.log(json);
    populateForm(json);
  }

  /**
   * Populates the form with components using JSON from the server.
   * The server builds a function model from the available config and encodes it as JSON.
   *
   * @param json represents the parts of the function model that need to be exposed to the user so they
   *   can enter configuration
   */
  function populateForm(json) {
    // TODO need config in here too
    var functions = json["functions"],
        colName = json["colName"],
        inputTypes = json["inputTypes"],
        inputType = json["inputType"],
        outputNames = json["outputNames"] || [],
        outputName = json["outputName"];

    $("#column-name").text(colName);
    populateInputTypeSelect(inputTypes, inputType);
    populateOutputNameSelect(outputNames, outputName);
    $("#output-name-row").nextAll("div").remove();

    if (!functions) {
      return;
    }
    var funcComponents = $.map(functions, function (fn) {
      var func = fn["func"], // function interface
          impl = fn["impl"], // selected implementation
          impls = fn["impls"] || [], // available implementations
          args = fn["args"] || []; // constructor arguments

      if (func) {
        if (impls.length == 0) {
          // function interface with no known implementations
          return createInterfaceComponents(func, [], null, []);
        }
        if (impls.length == 1 && args.length > 0) {
          // function whose type can be inferred but requires arguments
          return createImplementationComponents(func, impls[0], args);
        }
        if (impls.length > 1) {
          // user needs to choose the type and possibly enter the arguments
          return createInterfaceComponents(func, impls, impl, args);
        }
      } else if (impl && args.length > 0) {
        return createImplementationComponents(null, impl, args);
      }
    });
    $("#config").append(funcComponents);
  }

  /**
   * Populates the select for selecting the output name.
   *
   * @param values array of available output names
   * @param selValue the selected output name, null if there is no value selected
   */
  function populateOutputNameSelect(values, selValue) {
    var options = $.map(values, function (value) {
      return $("<option>", {value: value}).text(value);
    });
    var select = $("#output-name-select").empty();
    if (!selValue) {
      select.append("<option>");
    }
    select.append(options);

    if (selValue) {
      select.val(selValue);
    }
  }

  /**
   * Populates the select for selecting the input type.
   *
   * @param values array of available input types, {name: ..., type: ...}
   * @param selValue the selected value, null is there is no value selected
   */
  function populateInputTypeSelect(values, selValue) {
    var select = $("#input-type-select"),
        label = $("#input-type-label"),
        options;

    if (editing) {
      // if we're editing an existing output the input type can't be changed
      label.text(selValue ? selValue.name : "");
      select.hide();
      label.show();
    } else {
      // if we're adding a new output the input type can be selected from a drop-down
      options = $.map(values, function (value) {
        return $("<option>", {value: value["type"]}).text(value["name"]);
      });
      select.empty();
      if (!selValue) {
        select.append("<option>");
      }
      select.append(options);

      if (selValue) {
        select.val(selValue["type"]);
      }
      select.show();
      label.hide();
    }
  }

  /**
   *
   *
   * @returns the config in the format
   * <pre>
   *   {
   *     impls: {interface1: impl1, interface2: impl2, ... },
   *     args: {
   *         impl1: {
   *         propertyName1: arg1,
   *         propertyName2: arg2,
   *         ...
   *       },
   *       impl2: {
   *         propertyName3: arg3,
   *         ...
   *       },
   *       ...
   *     }
   *   }
   * </pre>
   */
  function updateConfig() {
    // array of objects [{name: ..., value: ...}, ...]
    var formValues = $("form").serializeArray();

    $.each(formValues, function (idx, item) {
      var argName, fnType, implType, implArgs, argValue,
          split = item.name.split("/", 2);

      if (item.name == "input-type") {
        config.inputType = item.value;
      } else if (item.name == "output-name") {
        config.outputName = item.value;
      } else if (split.length == 1) {
        // it's a function implementation,
        //   name = function interface class
        //   value = function implementation class
        fnType = item.name;
        implType = item.value;
        config.impls[fnType] = implType;
      } else {
        // it's an argument
        //   name = function implementation class / parameter name
        //   value = argument value
        implType = split[0];
        argName = split[1];
        argValue = item.value;
        implArgs = (config.args[implType] || {});
        implArgs[argName] = argValue;
        config.args[implType] = implArgs;
      }
    });
    return config;
  }

  /**
   * Updates the configuration using the values in the form and submits them to the server.
   * The server builds a function model and encodes the results as JSON in the response.
   */
  function submitConfig() {
    updateConfig();
    console.log("submitting config");
    console.log(config);
    $.post(modelEndpointUrl, JSON.stringify(config), function (response, textStatus, jqXHR) {
      receivedModel(jqXHR.responseText);
    });
  }

  return {
    initialize: initialize,
    updateConfig: updateConfig
  }
})();
