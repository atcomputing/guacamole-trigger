angular.module('guacTrigger').controller('hostController', ['$scope', '$routeParams', '$injector', '$interval',
    function hostController($scope, $routeParams, $injector, $interval) {


    var hostREST                 = $injector.get('hostREST');
    var guacClientManager        = $injector.get('guacClientManager');

    $scope.showBootNotification = false;

    var defaultHost = {hostname: "Host",
                       status: "UNSET"}

    $scope.host = defaultHost;
    function setHoststate() {


        // there is already loggig that when connect there are no notifications
        if ( $scope.host.status === "BOOTING"  || ! $scope.client || ["WAITING","CLIENT_ERROR"].includes($scope.client.clientState.connectionState)){
            startPollingHost();
        } else {
            stopPollingHost();
        }
        if (! $scope.client|| ! $scope.client.tunnel || !$scope.client.tunnel.uuid) {
            return
        }
        hostREST.getHost($scope.client.tunnel.uuid).then(
            function setHost(host){

                if (host){
                    $scope.host = host
                }
                $scope.showBootNotification = ($scope.host.status === "BOOTING")

            },
            function unknowHost(){$scope.host = defaultHost} );
    }

    var pollingHost;

    function startPollingHost() {

        if ( angular.isDefined(pollingHost) ) return;
        pollingHost = $interval(setHoststate, 500)

    }
    function stopPollingHost(){

        if (angular.isDefined(pollingHost)) {
            $interval.cancel(pollingHost);
            pollingHost = undefined;
        }
    }

    // $scope.client = guacClientManager.getManagedClient($routeParams.id);
    $scope.$watch(
        function () {
            var client = guacClientManager.getManagedClients()[$routeParams.id]
            if (client && client.clientState) {
                $scope.client = client
                return $scope.client.clientState.connectionState;
            } else {
                return null
            }

        },function () {

            setHoststate();
        });
}]);
