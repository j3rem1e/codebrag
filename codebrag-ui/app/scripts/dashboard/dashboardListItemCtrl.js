angular.module('codebrag.dashboard')

    .controller('DashboardListItemCtrl', function ($scope, $state, $stateParams, dashboardService, $rootScope, events) {

        $scope.openReactionDetails = function (reaction) {
            if(_thisReactionOpened(reaction)) {
                $rootScope.$broadcast(events.scrollToComment, reaction.id);
            } else {
                $state.transitionTo('dashboard.details', {commentId: reaction.id, commitId: reaction.commitId });
            }
        };
 
        function _thisReactionOpened(reaction) {
            return $state.current.name === 'dashboard.details' && $state.params.commitId === reaction.commitId;
        }


    });