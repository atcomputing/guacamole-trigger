angular.module('guacTrigger').controller('idleController', ['$scope', '$injector',
    function idleController($scope, $injector) {

    var idleServices             = $injector.get('idleService');
    var guacClientManager        = $injector.get('guacClientManager');
    var idleConfigREST           = $injector.get('idleConfigREST');

    // TODO add countdown or mention time when disconect

    $scope.idle = false;

    function setTimers(config){
        // TODO do we still set a timer if there is a notification (connection error)

        if (config.idleTime !== 0 && config.disconnectTime !== 0){
        idleServices.idleCallback(config.idleTime * 1000,
            function (){
                $scope.idle = true;
            },function (){
                $scope.idle=false
            });
        }

        if (config.disconnectTime !== 0){
            idleServices.idleCallback(config.disconnectTime* 1000 ,disconnect);
        }
    }
    idleConfigREST.getConfig().then(setTimers);

    function disconnect () {
        console.log("disconnect");
        guacClientManager.clear(); // this wont work because, guacd will  try to reconnect so disconnect event is never trigger in guacamole
        $scope.idle = false;
    }
}]);
