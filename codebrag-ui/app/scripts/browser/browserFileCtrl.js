angular.module('codebrag.browser').controller('BrowserFileCtrl', function ($scope, $stateParams, browserService, Comments) {

	function hashCode(string) {
	  var hash = 0, i, chr, len;
	  if (string.length === 0) return hash;
	  for (i = 0, len = string.length; i < len; i++) {
	    chr   = string.charCodeAt(i);
	    hash  = ((hash << 5) - hash) + chr;
	    hash |= 0; // Convert to 32bit integer
	  }
	  return hash;
	}
	
	function generateCommitColorID(file) {
		file.lines.forEach(function(line) {
			var uid = Math.abs(hashCode(line.sha));
			var hue = uid % 360;
			var sat = 30 + ((uid / 360) % 40)
			
			line.commitColor = "hsl(" + hue + "," + sat + "%,80%)";
		});
	}
	
	$scope.fileName = $stateParams.path;
	$scope.slashyFileName = $stateParams.path == '' ? '' : '/' + $stateParams.path;
	
	function buildPartialPath(path) {
		var result = [];
		var current = '';
		path.split('/').forEach(function(part) {
			var full = current + part;
			result.push({part: part, path:full});
			current = full + '/';
		});
		return result;
	}
	
	$scope.paths = buildPartialPath($stateParams.path);
	
	function findCommitBySha(id) {
		return $scope.file.commits.find(function(c) { return c.sha === id });
	}
	
	$scope.findCommitBySha = findCommitBySha;
	
	function ensureReactionsCollectionExists(lineNumber, reactionType) {
        if(_.isUndefined($scope.file.reactions[lineNumber])) {
        	$scope.file.reactions[lineNumber] = [];
        }
        if(_.isUndefined($scope.file.reactions[lineNumber][reactionType])) {
        	$scope.file.reactions[lineNumber][reactionType] = [];
        }
    }

	$scope.submitInlineComment = function(content, commentData) {
		
		var currentLine = $scope.file.lines[commentData.lineNumber]
		
        var newComment = {
                commitId: findCommitBySha(currentLine.sha).id,
                body: content,
                fileName: $scope.fileName,
                lineNumber: currentLine.diffLineNumber
            };

        return Comments.save(newComment).$then(function (commentResponse) {
            var comment = commentResponse.data.comment;
            
            ensureReactionsCollectionExists(commentData.lineNumber, 'comments');
            $scope.file.reactions[commentData.lineNumber].comments.push(comment);
        });
	};
	
	$scope.repoName = $stateParams.repoName;
	$scope.commitId = $stateParams.commitId;

  	browserService.loadFile($stateParams.repoName, $stateParams.commitId, $stateParams.path).then(function(response) {
  	
  		if (response.data.lines) {
  			generateCommitColorID(response.data);
  		}
  	
  		$scope.file = response.data;
  		$scope.file.name = $stateParams.path;
  		$scope.file.paths = $scope.paths;
  		$scope.file.repoName = $scope.repoName;
  		$scope.file.commitId = $scope.commitId;
  	});
}).directive("lineBlame", function($state, $stateParams, $sanitize) {
	return {
        restrict: 'A',
        link: function(scope, el) {
            var fileDiffRootSelector = 'table';
            var hoverSelector = '[data-blame-commit]';
            
        	var fileDiffRoot = el.parent(fileDiffRootSelector);
        	fileDiffRoot.on('mouseenter', hoverSelector, function(event) {
        		var el = $(event.currentTarget).find("div");
        		
        		var commit = scope.findCommitBySha($(event.currentTarget).data("blame-commit"));
        		
        		var headline = commit.message.split(/\n+/)[0] || 'no headline';
        		var detailed = '';
        		var parts = commit.message.split(/\n+/);
                if (parts.length > 1) {
                    parts.shift();
                    detailed = parts.join('<br>');
                }
                
                var date = moment(commit.date).add(-1, 'hour').fromNow();
                
                // TODO: Angular template...
                var detailedHtml = '<div class="authored-box"><div class="author-box"><img src="' + 
                commit.authorAvatarUrl + 
                '?d=https://raw.githubusercontent.com/softwaremill/codebrag/master/codebrag-ui/app/assets/images/avatar.png"></div>' +
                '<div class="info-box"><div><span>' + detailed + '</span></div></div>' +
                '<div class="info-line"><span class="username">' + commit.authorName + '</span><span class="time">' + date + '</span></div>' +
                '</div></div>';
                    
        		el.popover({title:headline, animation:false, content:$sanitize(detailedHtml), html:true});
        		el.popover('show');
        	});
        	fileDiffRoot.on('mouseleave', hoverSelector, function(event) {
        		$(event.currentTarget).find("div").popover('hide');
        	});
        	fileDiffRoot.on('click', hoverSelector, function(event) {
        		scope.$apply(function() {
        			$state.transitionTo('commits.details', {repo:$stateParams.repoName, sha:$(event.currentTarget).data("blame-commit")});
        		});
        	});
        }
    };
}).controller("BrowserBranchCtrl", function(branchesService, $scope, $stateParams, $state, events) {

	function update() {
		$scope.branches = [];
		$scope.commitId = $stateParams.commitId;
		$scope.userCommitId = $stateParams.commitId.replace(new RegExp("\\$", 'g'), '/');
	    
		branchesService.loadBranches($stateParams.repoName).then(function() {
			var branches = [];
			var re = new RegExp("/", 'g');
			var found = false;
			
			branchesService.branches.forEach(function(branch) {
				var current = {name:branch.name, path:branch.name.replace(re, '$')};
				branches.push(current);
				found |= $stateParams.commitId === current.path;
			});
			if (!found) {
				branches.push({name:$scope.userCommitId, path:$scope.commitId});
			}
			
			$scope.branches = branches;
		});		
	};
	
	update();
	$scope.$on(events.browserRepoChanged, update);
	
	$scope.isSelected = function(branch) {
		return $stateParams.commitId === branch.path;
	};
	
	$scope.selectBranch = function(branch) {
		$scope.commitId = branch.path;
		$scope.userCommitId = $stateParams.commitId.replace(new RegExp("\\$", 'g'), '/');
		$state.transitionTo("browser.file", {commitId:branch.path, repoName: $stateParams.repoName, path: $stateParams.path});
	};
});
