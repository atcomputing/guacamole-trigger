angular.module('guacTrigger').controller('hostController', ['$scope','$rootScope','$timeout', '$routeParams', '$injector', '$interval',
    function hostController($scope,$rootScope, $timeout, $routeParams, $injector, $interval) {

    console.log("trigger: host controler loaded")
    var hostREST                 = $injector.get('hostREST');
    var guacClientManager        = $injector.get('guacClientManager');
    var guacNotification         = $injector.get('guacNotification');
    $scope.showBootNotification = false;

    var defaultHost = {hostname: "Host",
                       status: "UNSET"}
    $scope.host = defaultHost;

    $scope.client = guacClientManager.getManagedClients()[$routeParams.id]
    function setHostState() {


        $scope.client = guacClientManager.getManagedClients()[$routeParams.id]
        

        if (! $scope.client|| ! $scope.client.tunnel || !$scope.client.tunnel.uuid ){

            console.log("stop missing")
            stopPollingHost();
            $timeout(setHostState, 2000);
            return
        }

        // TODO this does now work keeps polling for client with error
        if ($scope.client.id !== $routeParams.id) {

            console.log("stop wrong id")
            stopPollingHost()
            return
        }

        console.log($scope.$id + ":" + $scope.client.clientState.connectionState + " " + $scope.host.status)
        if ($scope.client.clientState.connectionState === "CONNECTED" || ! ["BOOTING","UNSET"].includes($scope.host.status)) {

            console.log("stop connect")
            $scope.showBootNotification = false
            stopPollingHost();
            return
        }

        if (["DISCONNECTED"].includes($scope.client.clientState.connectionState)) {

            console.log("reconnect")
            console.log($scope.client)

            // if ($scope.showBootNotification = true && $scope.client.clientState.connectionState === 'CLIENT_ERROR' ){
                $scope.client = guacClientManager.replaceManagedClient($scope.client.id);
                $scope.showBootNotification = false
            // }
            // console.log("stop reconnect")
            stopPollingHost();
            return
        }
        hostREST.getHost($scope.client.tunnel.uuid).then(
            function setHost(host){

                if (host){
                    // ugly
                    $scope.host = host
                }
                $scope.showBootNotification = ($scope.host.status === "BOOTING")
                if($scope.host.status === "BOOTING"  && $scope.client.clientState.connectionState !== "CONNECTED"){
                    startPollingHost();
                }
                console.log(host)
            },
            function unknowTunnel(e) {
                console.log("failed finding host status")
                stopPollingHost();
                $scope.host = defaultHost}
             );
    }

    var pollingHost;
    function startPollingHost() {

        // console.log(guacNotification.getStatus());
        // guacNotification.showStatus(false);
        if ( angular.isDefined(pollingHost) ) {
            return;
        }
        console.log("trigger: start polling");
        pollingHost = $interval(setHostState, 1000)
    }
    function stopPollingHost(){

        console.log("trigger " +$scope.$id+ ": stop polling");
        if (angular.isDefined(pollingHost)) {
            $interval.cancel(pollingHost);
            pollingHost = undefined;
        }
    }

    // $scope.client = guacClientManager.getManagedClient($routeParams.id);
    $scope.$watchGroup([
        'client.clientState.connectionState',
    ], function test (newvar){
        setHostState()
    })
    // function setHostTwice(){
    //     setHostState()
    //     $timeout(setHostState, 2000);
    // }
    // $scope.$watch(
    //     function () {
    //
    //         // var client = guacClientManager.getManagedClients()[$routeParams.id]
    //
    //         if ($scope.client && $scope.client.clientState) {
    //             return $scope.client.clientState.connectionState;
    //         } else {
    //             return null
    //         }
    //
    //     },function () {
    //
    //         setHostTwice();
    //     });
    setHostState();
}]);
