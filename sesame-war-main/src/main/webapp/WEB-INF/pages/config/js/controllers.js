var configApp = angular.module("configApp", ["ngRoute"]);

/**
 * Controller for listing all columns in the configuration database.
 */
configApp.controller("ListColumnsCtrl", function ($scope, $http) {
  $http.get("/columns").success(function (data) {
    console.log(data);
    $scope.columns = data.columns;
  });
  $scope.deleteColumn = function (columnId) {
    $http.delete("/columns/" + columnId).success(function () {
      $scope.columns = $.grep($scope.columns, function (column) {
        return column.id != columnId;
      });
    });
    // TODO need to redo the classes for the table stripes. what's the official angular way?
  };
});

/**
 * Controller for adding a new column.
 */
configApp.controller("AddColumnCtrl", function ($scope, $http, $location) {
  $scope.saveColumn = function () {
    if ($scope.columnName) {
      $scope.error = null;

      $http.post("/columns", $scope.columnName).success(function (data) {
        $location.path("/columns/" + data);
      }).error(function (data) {
        $scope.error = data;
      });
    } else {
      $scope.error = "Column name is required";
    }
  }
});

/**
 * Controller for editing the outputs on an existing column.
 */
configApp.controller("EditColumnCtrl", function ($scope, $http, $routeParams) {
  $http.get("/columns/" + $routeParams.columnId).success(function (data) {
    console.log(data);
    $scope.inputs = data.inputTypes;
    $scope.column = {name: data.name, id: data.id};
    $scope.updateName = function () {
      alert("updating name to " + $scope.column.name);
    };
    $scope.deleteOutput = function (inputType) {
      $http.delete("/columns/" + $routeParams.columnId + "/" + inputType).success(function () {
        $scope.inputs = $.grep($scope.inputs, function (input) {
          return input.type != inputType;
        })
      });
    };
  });
});

/**
 * Controller for configuring the functions that will calculate a single output.
 */
configApp.controller("ConfigCtrl", function ($scope, $routeParams, $location, $http) {
  $(document).trigger("buildConfigForm", [$routeParams.columnId, ($routeParams.inputType || null)]);
  $scope.saveConfig = function () {
    // get the config JSON from the page
    var config = configModule.updateConfig(),
        url = "/columns/" + $routeParams.columnId + "/" + config.inputType,
        configJson = JSON.stringify(config);

    if (!config.inputType) {
      // TODO set something in the scope to show an error message
      alert("input type not selected");
      return;
    }
    $http.put(url, configJson).success(function () {
      $location.path("/columns/" + $routeParams.columnId);
    }).error(function () {
      // TODO failure function to add an error message to the model
      alert("Failed to save configuration");
    });
    console.log(config);
  }
});

/**
 * Maps URLs to controllers.
 */
configApp.config(['$routeProvider', function ($routeProvider) {
  $routeProvider
      // list all columns
      .when("/columns", {templateUrl: "columns.html", controller: "ListColumnsCtrl"})
      // add a new column
      .when("/columns/add", {templateUrl: "addcolumn.html", controller: "AddColumnCtrl"})
      // show configured outputs for a column
      .when("/columns/:columnId", {templateUrl: "column.html", controller: "EditColumnCtrl"})
      // add config to a column for a new input type
      .when("/columns/:columnId/add", {templateUrl: "config.html", controller: "ConfigCtrl"})
      // edit existing config for an output
      .when("/columns/:columnId/:inputType", {templateUrl: "config.html", controller: "ConfigCtrl"})
      .otherwise({redirectTo: "/columns"});
}]);
