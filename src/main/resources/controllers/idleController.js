angular.module('guacTrigger').controller('idleController', ['$scope', '$routeParams','$injector',
    function idleController($scope, $routeParams, $injector) {

    var idleServices             = $injector.get('idleService');
    var guacClientManager        = $injector.get('guacClientManager');

    $scope.messages = "test2 een twee drie ";
    $scope.idle = false;


    idleServices.idleCallback(10000,
        function (){
            $scope.idle = true;
            $scope.$apply();
        },function (){
            $scope.idle=false
            $scope.$apply();
        });
    idleServices.idleCallback(20000,disconnect);

    function disconnect () {
        // TODO disconnect all. not only current
        guacClientManager.getManagedClient($routeParams.id).client.disconnect();
        $scope.idle = false;
    }
}]);
