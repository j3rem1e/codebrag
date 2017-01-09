angular.module('codebrag.dashboard')

    .controller('DashboardDetailsCtrl', function ($stateParams, $state, $scope, dashboardService, commitsService) {

        var commentId = $stateParams.commentId;
        $stateParams.followupId = undefined;
        
        $scope.scrollTo = $stateParams.commentId;

        dashboardService.getCommitFromCommentId(commentId).then(function(result) {
            commitsService.commitDetails(result.commit.sha, result.commit.repoName).then(function(commit) {
                $scope.currentCommit = new codebrag.CurrentCommit(commit);
            });
        });


    });