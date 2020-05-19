angular.module('guacTrigger').factory('hostREST', ['$injector',
        function quickConnectService($injector) {

    // Required services
    var authenticationService = $injector.get('authenticationService');
    var requestService        = $injector.get('requestService');

    var service = {};

    service.getHost = function getHost(host){

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve active connection
        return requestService({
            method  : 'GET',
            url     : 'api/session/ext/trigger/host/' + encodeURIComponent(host),
            params  : httpParameters
        });

    };

    return service;
}]);
