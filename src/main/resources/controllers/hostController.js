angular.module('guacTrigger').controller('hostController', ['$scope','$rootScope','$timeout', '$routeParams', '$injector',
  function hostController($scope,$rootScope, $timeout, $routeParams, $injector) {


    var hostREST                 = $injector.get('hostREST');
    var guacClientManager        = $injector.get('guacClientManager');
    // var managedClient            = $injector.get('ManagedClient');
    // var guacNotification         = $injector.get('guacNotification');

    var defaultHost = {
      hostname: "Host",
      console: "",
      status: "UNSET"
    };

    $rootScope.showBootNotification = $rootScope.showBootNotification || false;
    $rootScope.host = $rootScope.host || defaultHost;

    // console.log("trigger: host controler loaded"); // TODO remove messages
    // console.log($rootScope.showBootNotification);
    // console.log($rootScope.host);

    function pollHost(client){
      let pollCounter = 0;
      function poll() {
        pollCounter++;
        // console.log(client);
        // console.log(client.clientState.connectionState);
        // console.log($rootScope.showBootNotification);

        // wrong id stop
        if (client.id && client.id !== $routeParams.id) {

          console.log("stop wrong id");
          $rootScope.showBootNotification = false;
          return;
        }

        // missing stop
        // this should already be checked by waitForClient. but do hit here anyway
        if (! client || !client.id ||  !client.tunnel || !client.tunnel.uuid ){

          console.log("stop missing");
          console.log(client);
          $rootScope.showBootNotification = false;
          return;
        }

        if (["DISCONNECTED", "CLIENT_ERROR"].includes(client.clientState.connectionState)) {
          console.log("reconnect");
          console.log(pollCounter);
          if (pollCounter > 1) { // dont reconnect if you just connecting
            guacClientManager.replaceManagedClient(client.id);
          }
          return;
        }

        // success stop
        if (client.clientState.connectionState === "CONNECTED" ){

          // console.log("stop connect");
          $rootScope.showBootNotification = false;
          return;
        }

        hostREST.getHost(client.tunnel.uuid).then(
          function foundHost(host){

            if (host){
              // console.log(host);
              $rootScope.host = host;
              $rootScope.showBootNotification = ["BOOTING","TERMINATING"].includes(host.status);
              if (pollCounter > 600) { // limit count some way
                $rootScope.showBootNotification = false;
              }
              if ($rootScope.showBootNotification) {
                $timeout(poll,1000);
              }
            }
          },
          function unknowTunnel() {
            console.log("failed finding host status");
            $rootScope.host = defaultHost;
          }
        );
      }
      poll();
    }

    function waitForClient(){

      return new Promise((resolve, reject) => {
        let counter = 0;
        function wait() {
          counter++;
          // let client = guacClientManager.getManagedClient($routeParams.id);
          let managedClients = guacClientManager.getManagedClients();
          if ($routeParams.id in managedClients) {
            let client = managedClients[$routeParams.id];

            if (client.id && client.tunnel && client.tunnel.uuid) {

              resolve(client);
              return;
            }

          }
          if (counter < 100){ // limit waith
            $timeout(wait, 1000);
          } else {
            reject();
          }
        }
        wait();
      });
    }
    // for now dont watch connection state. only show boot messages after reconnect.
    // let cancelWatch = $scope.$watchGroup([
    //   'client',
    // ], function test (newvar){
    //   console.log("watch:");
    //   console.log(newvar);
    //   setHostState(newvar);
    // });
    waitForClient()
      .then(pollHost)
      .catch(() => $rootScope.showBootNotification = false);
  }]);
