angular.module('guacTrigger', []);

// Ensure the guaTrigger module is loaded along with the rest of the app
angular.module('index').requires.push('guacTrigger');
