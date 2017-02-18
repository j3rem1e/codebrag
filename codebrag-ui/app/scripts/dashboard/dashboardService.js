angular.module('codebrag.dashboard')

    .factory('dashboardService', function($http, $rootScope, $q, events) {

        var commitsListLocal = new codebrag.dashboard.LocalListCommits();
        var listFetched = false;
        
        var currentRequest;

        var parameters = 'watched';

        function load(param) {
            currentRequest = _httpRequest(param).then(function(response) {
                commitsListLocal.addAll(response.data.eventCommitsView);
                listFetched = true;
                return commitsListLocal.collection;
            });
            return currentRequest;
        }


        function allEvents() {
        	return load();
        }

        function myEvents() {
            return load('user');
        }

        function loadWatchedRepositories() {
            return load('watched');
        }
        
        function loadEventsIfNecessary() {
            if (currentRequest) {
            	return currentRequest;
            } else {
            	return allEvents();
            }
        }

        function loadWatchedEventsIfNecessary() {
            if (currentRequest) {
                return currentRequest;
            } else {
                return loadWatchedRepositories();
            }
        }

        function getCommitFromCommentId(commentId) {
        	return loadEventsIfNecessary().then(function() {
        	    var c = commitsListLocal.getCommitFromCommentId(commentId);
        	    if (c !== undefined) {
        	        return c;
        	    }
        	    return allEvents().then(function() {
        	        return commitsListLocal.getCommitFromCommentId(commentId);
        	    });
        	});
        }

        function _httpRequest(param, config) {
            var dashboardUrl = 'rest/dashboard';
            if (param) {
                dashboardUrl = dashboardUrl + '?' + param;
            }
            parameters = param;
            var reqConfig = angular.extend(config || {}, {method: 'GET', url: dashboardUrl});
            return $http(reqConfig);
        }

        function triggerCounterDecrease() {
            $rootScope.$broadcast(events.followupDone);
        }

        function refresh() {
            return load(parameters);
        }

        return {
        	allEvents: allEvents,
        	loadMyComments: myEvents,
        	loadWatchedRepositories: loadWatchedRepositories,
        	loadWatchedEventsIfNecessary: loadWatchedEventsIfNecessary,
        	refresh: refresh,
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
