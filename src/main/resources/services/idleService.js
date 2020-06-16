angular.module('guacTrigger').factory('idleService', ['$timeout','$document',
        function idleService($timeout, $document) {

    var service = {};

    var waitingFor = 0;
    var idleCallbacks = [];
    var TimeOut_Thread = null;

    function wakeup (){
        var idleCallback = idleCallbacks[waitingFor].idleCallback();
        if (idleCallback){idleCallback()}


        waitingFor +=1;
        if (waitingFor < idleCallbacks.length ){
            TimeOut_Thread = $timeout(function(){wakeup()}, idleCallbacks[waitingFor].time - idleCallbacks[waitingFor-1].time );
        }
    }
    service.idleCallback = function (idleTime,idleCallback,activeCallback) {
        if (!TimeOut_Thread){

            TimeOut_Thread = $timeout(function(){wakeup()}, idleTime);

            var bodyElement = angular.element($document);

            angular.forEach(['keydown', 'keyup', 'click', 'mousemove', 'DOMMouseScroll', 'mousewheel', 'mousedown', 'touchstart', 'touchmove', 'scroll', 'focus'],
            function(EventName) {
                 bodyElement.bind(EventName, function (e) { service.resetCountDown(e) });
            });
        }
        idleCallbacks.push ({time:idleTime,
                             idleCallback: idleCallback,
                             activeCallback:activeCallback});
        idleCallbacks.sort(function(a,b){return a.time - b.time});

    }

    service.resetCountDown = function resetCountDown(){

        /// Stop the pending timeout
        $timeout.cancel(TimeOut_Thread);

        /// Reset the timeout
        TimeOut_Thread = $timeout(function(){wakeup()}, idleCallbacks[0].time);
        while (waitingFor > 0){
            waitingFor -=1
            var activeCallback = idleCallbacks[waitingFor].activeCallback
            if (activeCallback) {activeCallback()}

        }
    }

    // TODO
    // service.remove()

    return service;
}]);
