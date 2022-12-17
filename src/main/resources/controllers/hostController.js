angular.module('guacTrigger').controller('hostController', ['$scope', '$routeParams', '$injector', '$interval',
    function hostController($scope, $routeParams, $injector, $interval) {

    console.log("trigger: host controler loaded")
    var hostREST                 = $injector.get('hostREST');
    var guacClientManager        = $injector.get('guacClientManager');
    var guacNotification         = $injector.get('guacNotification');
    $scope.showBootNotification = false;

    var defaultHost = {hostname: "Host",
                       status: "UNSET"}

    $scope.host = defaultHost;
    function setHoststate() {


        console.log("trigger: update host state");
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

    // TODO set hard limit on how often polling is done
    var pollingHost;

    function startPollingHost() {

        console.log(guacNotification.getStatus());
        guacNotification.showStatus(false);
        console.log("trigger: start polling");
        if ( angular.isDefined(pollingHost) ) return;
        pollingHost = $interval(setHoststate, 500)

    }
    function stopPollingHost(){

        console.log("trigger: stop polling");
        if (angular.isDefined(pollingHost)) {
            $interval.cancel(pollingHost);
            pollingHost = undefined;
        }
    }

    // $scope.client = guacClientManager.getManagedClient($routeParams.id);
    $scope.$watch(
        function () {

            var client = guacClientManager.getManagedClients()[$routeParams.id]

            console.log(guacNotification.getStatus());
            guacNotification.showStatus(false);
            console.log(client);
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
