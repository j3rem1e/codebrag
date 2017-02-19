angular.module('codebrag.session')

    .factory('configService', function ($http, $q) {

        var config;
        var p;

        return {
            fetchConfig: function () {
                if(p) {
                    return p;
                }
                p = $http.get('rest/config/').then(function(resp) {
                    config = resp.data;
                    return config;
                });
                return p;
            }
        };

    });