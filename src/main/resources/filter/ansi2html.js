angular.module('guacTrigger').filter('ansi2html',['$sce', function ($sce){

    ansi =  new AnsiUp
    return function(text){
        return $sce.trustAsHtml(ansi.ansi_to_html(text)) }
}]);
