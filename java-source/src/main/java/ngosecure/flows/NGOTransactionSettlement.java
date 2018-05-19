package ngosecure.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import ngosecure.contract.NGOSecureContract;
import ngosecure.flows.NGOBaseFlow.SignTxFlowNoChecking;
import net.corda.finance.contracts.asset.Cash;
import ngosecure.vo.NGOTransaction;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

import static net.corda.finance.contracts.GetBalances.getCashBalance;

public class NGOTransactionSettlement {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends NGOBaseFlow {
        private final UniqueIdentifier linearId;
        private final Amount<Currency> amount;
        private final Boolean anonymous;

        private final Step PREPARATION = new Step("Obtaining IOU from vault.");
        private final Step BUILDING = new Step("Building and verifying transaction.");
        private final Step SIGNING = new Step("Signing transaction.");
        private final Step COLLECTING = new Step("Collecting counterparty signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING = new Step("Finalising transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                PREPARATION, BUILDING, SIGNING, COLLECTING, FINALISING
        );

        public Initiator(UniqueIdentifier linearId, Amount<Currency> amount, Boolean anonymous) {
            this.linearId = linearId;
            this.amount = amount;
            this.anonymous = anonymous;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Stage 1. Retrieve ngotransaction specified by linearId from the vault.
            progressTracker.setCurrentStep(PREPARATION);
            final StateAndRef<NGOTransaction> ngotransactionToSettle = getNGOTransactionByLinearId(linearId);
            final NGOTransaction inputNGOTransaction = ngotransactionToSettle.getState().getData();

            // Stage 2. Resolve the lender and borrower identity if the ngotransaction is anonymous.
            final Party borrowerIdentity = resolveIdentity(inputNGOTransaction.getBorrower());
            final Party lenderIdentity = resolveIdentity(inputNGOTransaction.getLender());

            // Stage 3. This flow can only be initiated by the current recipient.
            if (!borrowerIdentity.equals(getOurIdentity())) {
                throw new FlowException("Settle NGOTransaction flow must be initiated by the borrower.");
            }

            // Stage 4. Check we have enough cash to settle the requested amount.
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), amount.getToken());
            final Amount<Currency> amountLeftToSettle = inputNGOTransaction.getAmount().minus(inputNGOTransaction.getPaid());
            if (cashBalance.getQuantity() <= 0L) {
                throw new FlowException(String.format("Borrower has no %s to settle.", amount.getToken()));
            } else if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format(
                        "Borrower has only %s but needs %s to settle.", cashBalance, amount));
            } else if (amountLeftToSettle.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format(
                        "There's only %s left to settle but you pledged %s.", amountLeftToSettle, amount));
            }

            // Stage 5. Create a settle command.
            final List<PublicKey> requiredSigners = inputNGOTransaction.getParticipantKeys();
            final Command settleCommand = new Command<>(new NGOSecureContract.Commands.Settle(), requiredSigners);

            // Stage 6. Create a transaction builder. Add the settle command and input ngotransaction.
            progressTracker.setCurrentStep(BUILDING);
            final TransactionBuilder builder = new TransactionBuilder(getFirstNotary())
                    .addInputState(ngotransactionToSettle)
                    .addCommand(settleCommand);

            // Stage 7. Get some cash from the vault and add a spend to our transaction builder.
            final List<PublicKey> cashSigningKeys = Cash.generateSpend(
                    getServiceHub(),
                    builder,
                    amount,
                    inputNGOTransaction.getLender(),
                    ImmutableSet.of()).getSecond();

            // Stage 8. Only add an output ngotransaction state if the ngotransaction has not been fully settled.
            final Amount<Currency> amountRemaining = amountLeftToSettle.minus(amount);
            if (amountRemaining.getQuantity() > 0) {
                NGOTransaction outputNGOTransaction = inputNGOTransaction.pay(amount);
                builder.addOutputState(outputNGOTransaction, NGOSecureContract.NGO_TXN_CONTRACT_ID);
            }

            // Stage 9. Verify and sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            builder.verify(getServiceHub());
            final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>()
                    .addAll(cashSigningKeys)
                    .add(inputNGOTransaction.getBorrower().getOwningKey())
                    .build();
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder, signingKeys);

            // Stage 10. Get counterparty signature.
            progressTracker.setCurrentStep(COLLECTING);
            final FlowSession session = initiateFlow(lenderIdentity);
            subFlow(new IdentitySyncFlow.Send(session, ptx.getTx()));
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    ImmutableSet.of(session),
                    signingKeys,
                    COLLECTING.childProgressTracker()));

            // Stage 11. Finalize the transaction.
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, FINALISING.childProgressTracker()));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            subFlow(new IdentitySyncFlow.Receive(otherFlow));
            SignedTransaction stx = subFlow(new SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
    }
}