angular.module('guacTrigger').directive('console', function () {
    console.log("trigger: loaded console")
    return {
        scope: {
            output: '=output'
        },
        template: '<p "text" class="console">{{output}} </p>' ,
        link: function (scope, element ) {

            scope.$watch('output',function (){
                // TODO find angularjs way to do this
                // auto scroll down on changes

                console.log("trigger: new output")
                document.getElementsByClassName("console")[0].scrollTop =100000
            })
        }
    };

});
