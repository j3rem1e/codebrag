'use strict';

describe("CommitsListService", function () {

    var $httpBackend,
        $rootScope,
        events,
        pendingCommitsListService,
        currentRepoContext = {
            branch: 'master',
            repo: 'codebrag'
        };

    beforeEach(module('codebrag.commits', function($provide) {
        $provide.value('currentRepoContext', currentRepoContext);
    }));

    beforeEach(inject(function (_$httpBackend_, _$rootScope_, _pendingCommitsListService_, _events_) {
        $httpBackend = _$httpBackend_;
        $rootScope = _$rootScope_;
        events = _events_;
        pendingCommitsListService = _pendingCommitsListService_;

        this.addMatchers({
            toEqualProps: function(expected) {
                return angular.equals(this.actual, expected);
            }
        });
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('should load pending commits with prefetch and update next/prev status', function() {
        // given
        var commits;
        var pendingCommitsResponse = buildCommitsResponse([1, 2, 3, 4, 5, 6, 7], 10, 0, 3);
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=7').respond(pendingCommitsResponse);
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=7').respond({commits: []}); // prefetch

        // when
        pendingCommitsListService.loadCommits().then(function(list) {
            commits = list;
        });
        $httpBackend.flush();

        // then
        expect(commits).toEqualProps(pendingCommitsResponse.commits);
        expect(pendingCommitsListService.hasNextCommits()).toBeFalsy();
        expect(pendingCommitsListService.hasPreviousCommits()).toBeFalsy();
    });


    it('should load next commits with prefetch', function() {
        // given
        var firstCommitsResponse = buildCommitsResponse([1, 2, 3, 4, 5, 6, 7], 30, 0, 23);
        var nextCommitsResponse = buildCommitsResponse([9, 10, 11, 12, 13, 14, 15], 30, 8, 15);
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=7').respond(firstCommitsResponse);
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=7').respond({commits: ['prefetched']});
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=7&min_sha=7').respond(nextCommitsResponse);
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=15').respond({commits: ['prefetched']});

        // when
        var commitsList;
        pendingCommitsListService.loadCommits().then(function(commits) {
            commitsList = commits;
            pendingCommitsListService.loadNextCommits();
        });
        $httpBackend.flush();

        // then
        var allLoadedCommitsCount = firstCommitsResponse.commits.length + nextCommitsResponse.commits.length;
        expect(commitsList.length).toBe(allLoadedCommitsCount);
    });

    it('mark commit as reviewed should append prefetched commit to list and prefetch next one', function() {
        // given
        var prefetchedCommitId = '123';
        var commitsResponse = buildCommitsResponse([1, 2, 3, 4, 5, 6, 7], 30, 0, 22);
        $httpBackend.whenGET('rest/commits/codebrag?branch=master&filter=to_review&limit=7').respond(commitsResponse);
        $httpBackend.whenGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=7').respond({commits: [{id: prefetchedCommitId, sha: prefetchedCommitId}]});
        $httpBackend.expectDELETE('rest/commits/codebrag/3').respond('');
        $httpBackend.expectGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=123').respond({commits: []});
        var commitIdToReview = 3;

        // when
        var commits;
        pendingCommitsListService.loadCommits().then(function(list) {
            commits = list;
            return pendingCommitsListService.markAsReviewed(commitIdToReview);
        }).then(function() {
            expect(commits.length).toBe(commitsResponse.commits.length);
            expect(commits.pop().id).toBe(prefetchedCommitId);
        });
        $httpBackend.flush();
    });

    it('should trigger counter refresh when commit marked as reviewed', function() {
        var prefetchedCommitId = '123';
        var commitsResponse = buildCommitsResponse([1, 2, 3, 4, 5, 6, 7], 8, 0, 5);
        $httpBackend.whenGET('rest/commits/codebrag?branch=master&filter=to_review&limit=7').respond(commitsResponse);
        $httpBackend.whenGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=7').respond({commits: [{id: prefetchedCommitId, sha: prefetchedCommitId}]});
        $httpBackend.whenDELETE('rest/commits/codebrag/3').respond('');
        $httpBackend.whenGET('rest/commits/codebrag?branch=master&filter=to_review&limit=1&min_sha=123').respond({commits: []});
        var commitIdToReview = 3;

        // when
        var commits;
        pendingCommitsListService.loadCommits().then(function(list) {
            commits = list;
            spyOn($rootScope, '$broadcast');
            return pendingCommitsListService.markAsReviewed(commitIdToReview).then(function() {
                expect($rootScope.$broadcast).toHaveBeenCalledWith(events.commitReviewed);
            });
        });
        $httpBackend.flush();
    });

    function buildCommitsResponse(commitsIds, totalCount, older, newer) {
        var commits = commitsIds.map(function(id) {
            return {id: id, sha: id, msg: 'Commit ' + id, pendingReview: true};
        });
        return {commits: commits, totalCount: totalCount, older: older, newer: newer};
    }

});
