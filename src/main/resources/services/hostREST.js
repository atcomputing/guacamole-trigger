angular.module('guacTrigger').factory('hostREST', ['$injector','$routeParams',
        function HostREST($injector,$routeParams) {

    // Required services
    var authenticationService = $injector.get('authenticationService');
    var requestService        = $injector.get('requestService');
    var idleConfigREST        = $injector.get('idleConfigREST');

    //TODO
    // var defaultHost = {hostname: "Host",
    //                    status: "UNSET"}
    var service = {};

    // service.getHost2 = function getHost2 (){
    //     $scope.client = guacClientManager.getManagedClients()[$routeParams.id]
    //
    // }
    service.getHost = function getHost(tunnelID){

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve active connection
        return requestService({
            method  : 'GET',
            url     : 'api/session/ext/trigger/host/' + encodeURIComponent(tunnelID),
            params  : httpParameters
        });

    };

    return service;
}]);
