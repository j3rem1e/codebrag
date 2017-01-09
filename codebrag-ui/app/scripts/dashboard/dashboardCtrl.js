angular.module('codebrag.dashboard')

    .controller('DashboardCtrl', function ($scope, $http, dashboardService, pageTourService, events) {

        $scope.$on(events.dashboardOpened, initCtrl);


        $scope.pageTourForFollowupsVisible = function() {
            return pageTourService.stepActive('dashboard') || pageTourService.stepActive('invites');
        };

        function initCtrl() {
        	dashboardService.allEvents().then(function(commits) {
                $scope.commits = commits;
            });
            $scope.hasFollowupsAvailable = true;
            $scope.mightHaveFollowups = true;
        }

        initCtrl();

    });