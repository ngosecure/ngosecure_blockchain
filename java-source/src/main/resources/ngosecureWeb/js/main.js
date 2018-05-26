"use strict";

// Define your backend here.
angular.module('demoAppModule', ['ui.bootstrap']).controller('DemoAppCtrl', function($http, $location, $uibModal,$timeout) {
    const demoApp = this;

    const apiBaseURL = "/api/ngosecure/";
    demoApp.insightbuttontext = "Insights";

    // Retrieves the identity of this and other nodes.
    let peers = [];
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);
    $http.get(apiBaseURL + "isnotary").then((response) => demoApp.isnotary = response.data);


    demoApp.openSubmitDonationModal = () => {
        const submitDonationModal = $uibModal.open({
            templateUrl: 'submitDonationModal.html',
            controller: 'SubmitDonationModalCtrl',
            controllerAs: 'submitDonationModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        // Ignores the modal result events.
        submitDonationModal.result.then(() => {}, () => {});
    };

    /** Displays the cash issuance modal. */
    demoApp.openIssueNGOCoinModal = () => {
        const issueNGOCoinModal = $uibModal.open({
            templateUrl: 'issueNGOCoinModal.html',
            controller: 'IssueNGOCoinModalCtrl',
            controllerAs: 'issueNGOCoinModal',
            resolve: {
                apiBaseURL: () => apiBaseURL
            }
        });

        issueNGOCoinModal.result.then(() => {}, () => {});
    };

    /** Displays the Txn transfer modal. */
    demoApp.openTransferModal = (id) => {
        const transferModal = $uibModal.open({
            templateUrl: 'transferModal.html',
            controller: 'TransferModalCtrl',
            controllerAs: 'transferModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                id: () => id
            }
        });

        transferModal.result.then(() => {}, () => {});
    };

    /** Displays the txn settlement modal. */
    demoApp.openSettleModal = (id,donor) => {
        const settleModal = $uibModal.open({
            templateUrl: 'settleModal.html',
            controller: 'SettleModalCtrl',
            controllerAs: 'settleModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                id: () => id,
                donor: () => donor
            }
        });

        settleModal.result.then(() => {}, () => {});
    };


    demoApp.insights = () => {
        // Check for NGO Secure insights
        demoApp.insightbuttontext = "Checking for insights";
        $http.get(apiBaseURL + "ngoSecureInsights").then((response) => demoApp.insights =
                                     response.data);
			   $timeout(function(){
               				angular.forEach(demoApp.insights, function (value, key) {
                       		demoApp.insightstate = key;
                       		demoApp.insightparties = value;
               				});
                    window.alert("New insights available from NGOSecure Insights engine !!");
                    demoApp.insightbuttontext = "Insights";
                }, 60000);

    }


    /** Refreshes the front-end. */
    demoApp.refresh = () => {
        // Update the list of NGO Txns.
        $http.get(apiBaseURL + "ngotransactions").then((response) => demoApp.activetxns =
            Object.keys(response.data).map((key) => response.data[key]));

        // Update the cash balances.
        $http.get(apiBaseURL + "cash-balances").then((response) => demoApp.cashBalances =
            response.data);

        //Retrieves the ledger transactions
        $http.get(apiBaseURL + "transactionledger").then((response) => demoApp.txns =
                    Object.keys(response.data).map((key) => response.data[key]));

    }

    demoApp.refresh();
});

// Causes the webapp to ignore unhandled modal dismissals.
angular.module('demoAppModule').config(['$qProvider', function($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);