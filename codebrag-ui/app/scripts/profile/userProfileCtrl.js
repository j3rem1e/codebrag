angular.module('codebrag.profile')

    .controller('UserProfileCtrl', function($scope, authService, $http) {

        authService.requestCurrentUser().then(function(user) {
            $scope.user = user;
        });

        $scope.saveFullName = function(user) {
            if (user.fullName && user.fullName.length > 0) {
                $http.put('rest/users/' + user.id + '/fullname', {fullname: user.fullName} ).then(function() {
                    $scope.userForm.$setPristine();
                });
            }
        };

    });