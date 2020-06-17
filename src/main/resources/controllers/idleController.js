angular.module('guacTrigger').controller('idleController', ['$scope', '$routeParams','$injector',
    function idleController($scope, $routeParams, $injector) {

    var idleServices             = $injector.get('idleService');
    var guacClientManager        = $injector.get('guacClientManager');
    var idleConfigREST        = $injector.get('idleConfigREST');

    $scope.messages = "test2 een twee drie ";
    $scope.idle = false;


    function setTimers(config){
        idleServices.idleCallback(config.idleTime * 1000,
            function (){
                $scope.idle = true;
                $scope.$apply();
            },function (){
                $scope.idle=false
                $scope.$apply();
            });

            idleServices.idleCallback(config.disconectTime* 1000 ,disconnect);
    }
    idleConfigREST.getConfig().then(setTimers);

    function disconnect () {
        // TODO disconnect all. not only current
        guacClientManager.getManagedClient($routeParams.id).client.disconnect();
        $scope.idle = false;
    }
}]);
