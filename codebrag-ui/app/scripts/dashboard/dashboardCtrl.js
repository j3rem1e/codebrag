angular.module('codebrag.dashboard')

    .controller('DashboardCtrl', function ($scope, $http, dashboardService, pageTourService, events) {

        $scope.$on(events.dashboardOpened, initCtrl);

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
            return pageTourService.stepActive('dashboard') || pageTourService.stepActive('invites');
        };

        function initCtrl() {
        	dashboardService.loadWatchedEventsIfNecessary().then(function(commits) {
        	    $scope.commits = commits;
        	});
            $scope.hasFollowupsAvailable = true;
            $scope.mightHaveFollowups = true;
        }

        initCtrl();

    });