angular.module('codebrag.dashboard')

    .controller('DashboardCtrl', function ($scope, dashboardService) {

        $scope.loadAll = function() {
            dashboardService.allEvents().then(function(commits) {
                 $scope.commits = commits;
            });
        };

       $scope.loadWatchedRepositories = function() {
            dashboardService.loadWatchedRepositories().then(function(commits) {
                 $scope.commits = commits;
            });
        };

       $scope.loadMyComments = function() {
            dashboardService.loadMyComments().then(function(commits) {
                 $scope.commits = commits;
            });
        };

        $scope.pageTourForFollowupsVisible = function() {
            return false;
        };

        function refresh() {
        	dashboardService.refresh().then(function(commits) {
        	    $scope.commits = commits;
        	});
            $scope.hasFollowupsAvailable = true;
            $scope.mightHaveFollowups = true;
        }

        var timer = setInterval(refresh, 30000);

        $scope.$on("$destroy", function() {
            clearInterval(timer);
        });

        refresh();

    });