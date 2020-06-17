angular.module('guacTrigger').factory('idleConfigREST', ['$injector',
        function idleConfigREST($injector) {

    // Required services
    var authenticationService = $injector.get('authenticationService');
    var requestService        = $injector.get('requestService');

    var service = {};

    service.getConfig = function getConfig(){

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve active connection
        return requestService({
            method  : 'GET',
            url     : 'api/session/ext/trigger/config/',
            params  : httpParameters
        });

    };

    return service;
}]);
