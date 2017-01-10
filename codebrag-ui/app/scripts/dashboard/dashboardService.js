angular.module('codebrag.dashboard')

    .factory('dashboardService', function($http, $rootScope, $q, events) {

        var commitsListLocal = new codebrag.dashboard.LocalListCommits();
        var listFetched = false;
        
        var currentRequest;

        function allEvents() {
        	currentRequest = _httpRequest('GET').then(function(response) {
            	commitsListLocal.addAll(response.data.eventCommitsView);
                listFetched = true;
                return commitsListLocal.collection;
            });
        	return currentRequest;
        }
        
        function loadEventsIfNecessary() {
            if (currentRequest) {
            	return currentRequest;
            } else {
            	return allEvents();
            }
        }

        function getCommitFromCommentId(commentId) {
        	return loadEventsIfNecessary().then(function() { return commitsListLocal.getCommitFromCommentId(commentId) });
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
        this.injectCommitId();
    };
    
    this.injectCommitId = function() {
    	_.each(this.collection, function(commit) {
    		var id = commit.sha;
    		_.each(commit.reactions, function(reaction) { reaction.commitId = id; });
    	});
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
