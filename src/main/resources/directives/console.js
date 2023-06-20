angular.module('guacTrigger').directive('console', function () {
  console.log("trigger: loaded console");
  return {
    scope: {
      output: '=output'
    },
    template: '<div class=console><p ng-bind-html="output | ansi2html"></p></div>' ,
    link: function (scope, element ) {

      scope.$watch('output',function (){

        // TODO Is this angularjs way to do this?
        // TODO Can preserve scroll postion?
        // TODO Can we prevent auto scroll if we are not scrolled to the bottum
        let el = document.getElementsByClassName("console")[0];
        el.scrollTop = 100000;
      });
    }
  };

});
