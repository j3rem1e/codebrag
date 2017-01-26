angular.module('codebrag.branches')

    .controller('ReposCtrl', function ($scope, $state, events, currentRepoContext, $rootScope) {

        $scope.repos = function() {
            return Object.getOwnPropertyNames(currentRepoContext.all);
        };

        $scope.selectRepo = function(selected) {
            currentRepoContext.switchRepo(selected);
            $state.transitionTo('commits.list', {repo: selected});
        };

        $scope.isSelected = function(repo) {
            return currentRepoContext.repo === repo;
        };

        $scope.selectedBranch = function() {
            return currentRepoContext.repo;
        };
        
        $scope.browseRepo = function(repo) {
        	
            $state.transitionTo('browser.file', {repoName: repo, path:'', commitId:currentRepoContext.all[repo].replace(new RegExp("/", 'g'), '$')}, true).then(function() {
            	$rootScope.$broadcast(events.browserRepoChanged);
            });
        };
        
        $scope.browseCurrentRepo = function() {
        	$scope.browseRepo(currentRepoContext.repo);
        };

    });


