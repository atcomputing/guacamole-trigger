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
        if ( $scope.host.status === "BOOTING"  || ["WAITING","CLIENT_ERROR"].includes($scope.client.clientState.connectionState)){
            startPollingHost();
        } else {
            stopPollingHost();
        }
        if (! $scope.client.tunnel.uuid) {
            // console.log("fail")
            $scope.client = guacClientManager.getManagedClient($routeParams.id);
            return
        }
        hostREST.getHost($scope.client.tunnel.uuid).then(
            function setHost(host){

                if (host){ $scope.host = host }
                // console.log("connection2: " + $scope.client.clientState.connectionState + " client: " + $scope.client.name + " status: " + $scope.host.status + " messages: " + host.console);
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

    $scope.client = guacClientManager.getManagedClient($routeParams.id);
    $scope.$watch(
        function () {
            return guacClientManager.getManagedClient($routeParams.id).clientState.connectionState;
        },function () {


            $scope.client = guacClientManager.getManagedClient($routeParams.id);

            // console.log("connection3: " + $scope.client.clientState.connectionState + "client: " + $scope.client.name + " status: " + $scope.host.status + " messages: " + $scope.host.console);
            setHoststate();
        });
}]);
