"use strict";

angular.module('demoAppModule').controller('SubmitDonationModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const submitDonationModal = this;

    submitDonationModal.peers = peers;
    submitDonationModal.form = {};
    submitDonationModal.formError = false;


    submitDonationModal.create = () => {
        if (invalidFormInput()) {
            submitDonationModal.formError = true;
        } else {
            submitDonationModal.formError = false;

            const amount = submitDonationModal.form.amount;
            const currency = "USD";//submitDonationModal.form.currency;
            const party = submitDonationModal.form.counterparty;

            $uibModalInstance.close();

            const issueNGOTransactionEndpoint =
                apiBaseURL +
                `create-transaction?amount=${amount}&currency=${currency}&party=${party}`;

            // We hit the endpoint to create the Txn and handle success/failure responses.
            $http.get(issueNGOTransactionEndpoint).then(
                (result) => submitDonationModal.displayMessage(result),
                (result) => submitDonationModal.displayMessage(result)
            );
        }
    };

    /** Displays the success/failure response from attempting to create a Txn. */
    submitDonationModal.displayMessage = (message) => {
        const submitDonationMsgModal = $uibModal.open({
            templateUrl: 'submitDonationMsgModal.html',
            controller: 'submitDonationMsgModalCtrl',
            controllerAs: 'submitDonationMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        submitDonationMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the Txn creation modal. */
    submitDonationModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the Txn input.
    function invalidFormInput() {
        return isNaN(submitDonationModal.form.amount) || (submitDonationModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('submitDonationMsgModalCtrl', function($uibModalInstance, message) {
    const submitDonationMsgModal = this;
    submitDonationMsgModal.message = message.data;
});