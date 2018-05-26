"use strict";

angular.module('demoAppModule').controller('IssueNGOCoinModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL,$window) {
    const issueNGOCoinModal = this;

    issueNGOCoinModal.form = {};
    issueNGOCoinModal.formError = false;

    issueNGOCoinModal.issue = () => {
        if (invalidFormInput()) {
            issueNGOCoinModal.formError = true;
        } else {
            issueNGOCoinModal.formError = false;

            const amount = issueNGOCoinModal.form.amount;
            const currency = "USD";//issueNGOCoinModal.form.currency;

            $uibModalInstance.close();

            const issueCashEndpoint =
                apiBaseURL +
                `self-issue-cash?amount=${amount}&currency=${currency}`;

            $http.get(issueCashEndpoint).then(
                (result) => {console.log(result.toString()); issueNGOCoinModal.displayMessage(result); },
                (result) => {console.log(result.toString()); issueNGOCoinModal.displayMessage(result); }
            );
        }
    };

    issueNGOCoinModal.displayMessage = (message) => {
        const issueNGOCoinMsgModal = $uibModal.open({
            templateUrl: 'issueNGOCoinMsgModal.html',
            controller: 'issueNGOCoinMsgModalCtrl',
            controllerAs: 'issueNGOCoinMsgModal',
            resolve: {
                message: () => message
            }
        });

        issueNGOCoinMsgModal.result.then(() => {}, () => {});
    };

    issueNGOCoinModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return isNaN(issueNGOCoinModal.form.amount);
     //   return isNaN(issueNGOCoinModal.form.amount) || (issueNGOCoinModal.form.currency.length != 3);
    }
});

angular.module('demoAppModule').controller('issueNGOCoinMsgModalCtrl', function($uibModalInstance, message) {
    const issueNGOCoinMsgModal = this;
    issueNGOCoinMsgModal.message = message.data;
});