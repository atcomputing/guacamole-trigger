angular.module('guacTrigger').controller('consoleController', ['$scope', '$routeParams', '$injector', '$interval',
    function consoleController($scope, $routeParams, $injector, $interval) {

    var connectionService        = $injector.get('connectionService');

    var hostREST                 = $injector.get('hostREST');

    var guacClientManager        = $injector.get('guacClientManager');


    $scope.showBootNotification = false;

    var defaultHost = {hostname: "Host",
                       status: "UNKNOW"}

    $scope.host = defaultHost
    var stop = $interval(function () {
        var connection = guacClientManager.getManagedClient($routeParams.id) ;
        if ( connection.name) {


            hostREST.getHost(connection.tunnel.uuid).then(
                function setHost(host){
                    $scope.host = host||defaultHost;
                    $scope.showBootNotification = ($scope.host.status === "BOOTING")

                },
                function unknowHost(){$scope.host = defaultHost} );
        }
    }, 5000)
}]);
