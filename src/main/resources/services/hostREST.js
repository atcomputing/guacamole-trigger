angular.module('guacTrigger').factory('hostREST', ['$injector','$routeParams',
  function HostREST($injector) {

    // Required services
    var authenticationService = $injector.get('authenticationService');
    var requestService        = $injector.get('requestService');

    var service = {};

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
