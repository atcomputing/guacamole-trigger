angular.module('guacTrigger').controller('idleController', ['$scope', '$injector',
    function idleController($scope, $injector) {

    var idleServices             = $injector.get('idleService');
    var guacClientManager        = $injector.get('guacClientManager');
    var idleConfigREST           = $injector.get('idleConfigREST');

    $scope.messages = "test2 een twee drie ";
    $scope.idle = false;


    function setTimers(config){
        idleServices.idleCallback(config.idleTime * 1000,
            function (){
                $scope.idle = true;
            },function (){
                $scope.idle=false
            });

            idleServices.idleCallback(config.disconectTime* 1000 ,disconnect);
    }
    idleConfigREST.getConfig().then(setTimers);

    function disconnect () {
        console.log("disconnect");
        guacClientManager.clear(); // this wont work because, guacd will  try to reconnect so disconnect event is never trigger in guacamole
        $scope.idle = false;
    }
}]);
