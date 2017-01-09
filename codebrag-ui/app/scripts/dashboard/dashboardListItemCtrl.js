angular.module('codebrag.dashboard')

    .controller('DashboardListItemCtrl', function ($scope, $state, $stateParams, dashboardService, $rootScope, events) {

        $scope.openReactionDetails = function (reaction) {
            if(_thisReactionOpened(reaction)) {
                $rootScope.$broadcast(events.scrollOnly);
            } else {
                $state.transitionTo('dashboard.details', {commentId: reaction.id});
            }
        };

        $scope.dismiss = function (followup) {
        	dashboardService.removeAndGetNext(followup.followupId).then(function(nextFollowup) {
                if(nextFollowup) {
                    $state.transitionTo('dashboard.details', {followupId: nextFollowup.followupId, commentId: nextFollowup.lastReaction.reactionId});
                } else {
                    $state.transitionTo('dashboard.list');
                }
            });
        };
 
        function _thisReactionOpened(reaction) {
            return $state.current.name === 'dashboard.details' && $state.params.commentId === reaction.id;
        }


    });