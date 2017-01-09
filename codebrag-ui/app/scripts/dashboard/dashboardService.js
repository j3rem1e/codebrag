angular.module('codebrag.dashboard')

    .factory('dashboardService', function($http, $rootScope, $q, events) {

        var commitsListLocal = new codebrag.dashboard.LocalListCommits();
        var listFetched = false;

        function allEvents() {
            return _httpRequest('GET').then(function(response) {
            	commitsListLocal.addAll(response.data.eventCommitsView);
                listFetched = true;
                return commitsListLocal.collection;
            });
        }
        
        function commitsList() {
        	if (listFetched) {
        		return $q.when(commitsListLocal);
        	} else {
        		return allEvents().then(function() { return commitsListLocal });
        	}
        }

        function getCommitFromCommentId(commentId) {
        	return commitsList().then(function(commits) { return commits.getCommitFromCommentId(commentId) });
        }

        function _httpRequest(method, config) {
            var dashboardUrl = 'rest/dashboard';
            var reqConfig = angular.extend(config || {}, {method: method, url: dashboardUrl});
            return $http(reqConfig);
        }

        function triggerCounterDecrease() {
            $rootScope.$broadcast(events.followupDone);
        }

        return {
        	allEvents: allEvents,
            getCommitFromCommentId: getCommitFromCommentId,
        };

    });

var codebrag = codebrag || {};
codebrag.dashboard = codebrag.dashboard || {};

codebrag.dashboard.LocalListCommits = function(collection) {

    var self = this;

    this.collection = collection || [];

    this.addAll = function(newCollection) {
        this.collection.length = 0;
        Array.prototype.push.apply(this.collection, newCollection);
    };
    
    this.getCommitFromCommentId = function(commentId) {
    	
    	var reaction;
    	var associatedCommit = _.find(this.collection, function(commit) {
    		var nestedReaction = _.find(commit.reactions, function(reaction) { return reaction.id === commentId; });
    		if (nestedReaction !== undefined) {
    			reaction = nestedReaction;
    			return true;
    		}
    	});
    	
    	return associatedCommit === undefined ? undefined : { commit: associatedCommit, reaction:reaction};
    	
    }

};
