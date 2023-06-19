angular.module('guacTrigger').controller('hostController', ['$scope','$rootScope','$timeout', '$routeParams', '$injector', '$interval',
  function hostController($scope,$rootScope, $timeout, $routeParams, $injector, $interval) {

    console.log("trigger: host controler loaded"); // TODO remove messages

    var hostREST                 = $injector.get('hostREST');
    var guacClientManager        = $injector.get('guacClientManager');
    // var guacNotification         = $injector.get('guacNotification');

    var defaultHost = {
      hostname: "Host",
      console: "",
      status: "UNSET"
    };

    $rootScope.showBootNotification = $rootScope.showBootNotification || false;
    $rootScope.host = $rootScope.host || defaultHost;

    // function recheckHost(){
    //
    //   $scope.client = guacClientManager.getManagedClients()[$routeParams.id];
    //
    //   if ( $scope.client && $scope.client.id && ( !$scope.client.tunnel || !$scope.client.tunnel.uuid )){
    //
    //     // $scope.client = guacClientManager.replaceManagedClient($scope.client.id);
    //     $rootScope.showBootNotification = false;
    //     return;
    //   } else {
    //
    //     // TODO this cause infinit loop.
    //     //  dont make it stop polling.
    //     //  keep a missing pol count,
    //     //  maybe try to reconnect once. if waith did not work
    //     $timeout(setHostState(), 2000);
    //   }
    // }

    function setHostState() {


      // does client need to be in scope, and can't it not be passed from watch?
      $scope.client = guacClientManager.getManagedClients()[$routeParams.id];

      if (! $scope.client || !$scope.client.id ||  !$scope.client.tunnel || !$scope.client.tunnel.uuid ){

        console.log("stop missing");
        stopPollingHost();
        // TODO make function with name like waith for client
        $timeout(setHostState, 2000);
        return;
      }

      // TODO this does now work keeps polling for client with error
      // TODO remove
      if ($scope.client.id !== $routeParams.id) {

        console.log("stop wrong id");
        stopPollingHost();
        return;
      }

      console.log($scope.$id + ":" + $scope.client.clientState.connectionState + " " + $rootScope.host.status);
      if ($scope.client.clientState.connectionState === "CONNECTED" || ! ["BOOTING","UNSET","TERMINATING"].includes($rootScope.host.status)) {

        console.log("stop connect");
        $rootScope.showBootNotification = false;
        stopPollingHost();
        return;
      }else {
        $rootScope.showBootNotification = true;
      }
      if ($scope.client.clientState.connectionState === 'CLIENT_ERROR' ){

        console.log("client_erro");
        console.log($scope.client);
        stopPollingHost();
        // $scope.client.client.connect();
        // $timeout( guacClientManager.replaceManagedClient($scope.client.id),2000);
        // $rootScope.showBootNotification = false;
      }
      if (["DISCONNECTED"].includes($scope.client.clientState.connectionState)) {

        console.log("reconnect");
        console.log($scope.client);

        // $scope.client.connect() ;
        // if ($scope.showBootNotification = true && $scope.client.clientState.connectionState === 'CLIENT_ERROR' ){
        // $scope.client = guacClientManager.replaceManagedClient($scope.client.id);
        $rootScope.showBootNotification = false;
        // }
        // console.log("stop reconnect")
        stopPollingHost();

        $timeout( guacClientManager.replaceManagedClient($scope.client.id),2000);
        return;
      }

      hostREST.getHost($scope.client.tunnel.uuid).then(
        function setHost(host){

          if (host){
            // ugly
            $rootScope.host = host;
          }
          $rootScope.showBootNotification = ($rootScope.host.status === "BOOTING");
          if($rootScope.host.status === "BOOTING"  && $scope.client.clientState.connectionState !== "CONNECTED"){
            startPollingHost();
          }

          console.log(host);
        },
        function unknowTunnel(e) {
          console.log("failed finding host status");
          stopPollingHost();
          $rootScope.host = defaultHost;
        }
      );
    }

    function startPollingHost() {

      // console.log(guacNotification.getStatus());
      // guacNotification.showStatus(false);
      if ( angular.isDefined($rootScope.pollingHost) ) {
        return;
      }
      console.log("trigger: start polling");
      $rootScope.pollingHost = $interval(setHostState, 1000);
    }
    function stopPollingHost(){

      console.log("trigger " +$scope.$id+ ": stop polling");
      if (angular.isDefined($rootScope.pollingHost)) {
        $interval.cancel($rootScope.pollingHost);
        $rootScope.pollingHost = undefined;
      }
    }

    // $scope.client = guacClientManager.getManagedClient($routeParams.id);
    $scope.$watchGroup([
      'client.clientState.connectionState',
    ], function test (newvar){
      setHostState();
    });
    setHostState();
  }]);
