angular.module('codebrag.favicon')

    .directive('tabNotifier', function($window, $rootScope, configService) {

        var REGULAR_FAVICON = 'assets/images/favicon.ico';
        var NOTIFY_FAVICON = 'assets/images/notification-favicon/favicon.ico';

        var REGULAR_TITLE = 'Codebrag';
        var NOTIFY_TITLE = '* Codebrag';

        return {
            restrict: 'A',
            controller: function() {

                configService.fetchConfig().then(function(config) {

                        REGULAR_TITLE = config.title;
                        NOTIFY_TITLE = '* ' + config.title;

                        setFavicon(REGULAR_FAVICON, REGULAR_TITLE);

                        $rootScope.$on('newCommitsNotificationsAvailable', function() {
                            setFavicon(NOTIFY_FAVICON, NOTIFY_TITLE);
                        });

                        $rootScope.$on('allCommitsNotificationsRead', function() {
                            setFavicon(REGULAR_FAVICON, REGULAR_TITLE);
                        });

                        $rootScope.$on('followupsNotificationAvailable', function() {
                            setFavicon(NOTIFY_FAVICON, NOTIFY_TITLE);
                        });

                        $rootScope.$on('followupsNotificationRead', function() {
                            setFavicon(REGULAR_FAVICON, REGULAR_TITLE);
                        });

                        function setFavicon(iconUrl, tabTitle) {
                            $window.favicon && $window.favicon.change(iconUrl, tabTitle);
                        }
                });

            }
        }
    });