"use strict";

angular.module('demoAppModule').controller('SettleModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, id, donor) {
    const settleModal = this;

    settleModal.donor = donor;
    settleModal.id = id;
    settleModal.form = {};
    settleModal.formError = false;

    settleModal.settle = () => {
        if (invalidFormInput()) {
            settleModal.formError = true;
        } else {
            settleModal.formError = false;

            const id = settleModal.id;
            const donor = settleModal.donor;
            const amount = settleModal.form.amount;
            const currency = "USD";//settleModal.form.currency;

            $uibModalInstance.close();

            const issueIOUEndpoint =
                apiBaseURL +
                `settle-transaction?id=${id}&amount=${amount}&currency=${currency}&donor=${donor}`;

            $http.get(issueIOUEndpoint).then(
                (result) => settleModal.displayMessage(result),
                (result) => settleModal.displayMessage(result)
            );
        }
    };

    settleModal.displayMessage = (message) => {
        const settleMsgModal = $uibModal.open({
            templateUrl: 'settleMsgModal.html',
            controller: 'settleMsgModalCtrl',
            controllerAs: 'settleMsgModal',
            resolve: {
                message: () => message
            }
        });

        settleMsgModal.result.then(() => {}, () => {});
    };

    settleModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return isNaN(settleModal.form.amount) ;
        //return isNaN(settleModal.form.amount) || (settleModal.form.currency.length != 3);
    }
});

angular.module('demoAppModule').controller('settleMsgModalCtrl', function($uibModalInstance, message) {
    const settleMsgModal = this;
    settleMsgModal.message = message.data;
});