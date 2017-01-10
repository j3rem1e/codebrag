angular.module('codebrag.dashboard')

    .controller('DashboardDetailsCtrl', function ($stateParams, $state, $scope, dashboardService, commitsService, $rootScope, events) {

        var commentId = $stateParams.commentId;
        
        $stateParams.commitId = undefined;
        
        $scope.scrollTo = $stateParams.commentId;

        dashboardService.getCommitFromCommentId(commentId).then(function(result) {
            commitsService.commitDetails(result.commit.sha, result.commit.repoName).then(function(commit) {
                $scope.currentCommit = new codebrag.CurrentCommit(commit);
            });
        });
        
        $scope.$on(events.scrollToComment, function(event, commentId) {
        	$scope.scrollTo = commentId;
        	$stateParams.commentId = commentId;
        	$rootScope.$broadcast(events.scrollOnly, commentId);
        });


    });