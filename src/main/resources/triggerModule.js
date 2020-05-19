/**
 * Module which provides handling for TOTP multi-factor authentication.
 */
angular.module('guacTrigger', []);

// Ensure the guacTOTP module is loaded along with the rest of the app
angular.module('index').requires.push('guacTrigger');
