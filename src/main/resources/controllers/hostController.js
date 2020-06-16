angular.module('guacTrigger').controller('hostController', ['$scope', '$routeParams', '$injector', '$interval',
    function hostController($scope, $routeParams, $injector, $interval) {


    var idleServices             = $injector.get('idleServices');
    var hostREST                 = $injector.get('hostREST');
    var guacClientManager        = $injector.get('guacClientManager');

    idleServices.idleCallback(5000,function () {console.log("idle 5000")},function () {console.log("active 5000")});
    idleServices.idleCallback(10000,function () {console.log("idle 10000")},function () {console.log("active 10000")});
    idleServices.idleCallback(15000,function () {console.log("idle 15000")});
    $scope.showBootNotification = false;

    var defaultHost = {hostname: "Host",
                       status: "UNKNOW"}

    function setHoststate() {

        if (! $scope.client.tunnel.uuid) {return}
        hostREST.getHost($scope.client.tunnel.uuid).then(
            function setHost(host){

                $scope.host = host||defaultHost;
                // console.log("client: " + $scope.client.name + " status: " + $scope.host.status + " messages: " + $scope.host.console);
                $scope.showBootNotification = ($scope.host.status === "BOOTING")

                if ( $scope.host.status === "BOOTING" || !$scope.client.name ){
                    startPollingHost();
                } else {
                    stopPollingHost();
                }
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
            return guacClientManager.getManagedClient($routeParams.id);
        },function () {
            $scope.client = guacClientManager.getManagedClient($routeParams.id);
            setHoststate();
        } );

    $scope.$watch('client.clientState.connectionState', function clientStateChanged(connectionState) {
        setHoststate();
        });
}]);
