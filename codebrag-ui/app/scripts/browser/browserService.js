angular.module('codebrag.browser').factory('browserService', function($http, $q, events) {
	
	return {
		loadFile: function(repo, sha, path) {
			return $http.get("/rest/commits/" + repo + "/" + sha + "/" + path);
		}
	}
});