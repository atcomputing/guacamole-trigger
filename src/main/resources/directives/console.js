angular.module('guacTrigger').directive('console', function () {
    console.log("trigger: loaded console")
    return {
        scope: {
            output: '=output'
        },
        template: '<div class=console><p ng-bind-html="output | ansi2html"></p></div>' ,
        link: function (scope, element ) {

            scope.$watch('output',function (){
                // TODO find angularjs way to do this
                // auto scroll down on changes
                // console.log(element)

                // console.log("trigger:" + scope.$id + " new output");
                // TODO get this from agurment link
                el=document.getElementsByClassName("console")[0];
                // console.log(el)
                // scroledDown = Math.abs(el.scrollHeight - el.clientHeight - el.scrollTop) < 10
                // console.log(scroledDown);
                el.scrollTop =100000;
                // scroledDown = Math.abs(el.scrollHeight - el.clientHeight - el.scrollTop) < 1
                // console.log(scroledDown);
            })
        }
    };

});
