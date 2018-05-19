package ngosecure.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import ngosecure.contract.NGOSecureContract;
import ngosecure.flows.NGOBaseFlow.SignTxFlowNoChecking;
import ngosecure.vo.NGOTransaction;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NGOTransactionTransfer {

    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends NGOBaseFlow {
        private final UniqueIdentifier linearId;
        private final Party newLender;
        private final Boolean anonymous;

        private final Step PREPARATION = new Step("Obtaining IOU from vault.");
        private final Step BUILDING = new Step("Building and verifying transaction.");
        private final Step SIGNING = new Step("Signing transaction.");
        private final Step SYNCING = new Step("Syncing identities.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return IdentitySyncFlow.Send.Companion.tracker();
            }
        };
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
                PREPARATION, BUILDING, SIGNING, SYNCING, COLLECTING, FINALISING
        );

        public Initiator(UniqueIdentifier linearId, Party newLender, Boolean anonymous) {
            this.linearId = linearId;
            this.newLender = newLender;
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
            final StateAndRef<NGOTransaction> ngotransactionToTransfer = getNGOTransactionByLinearId(linearId);
            final NGOTransaction inputNGOTransaction = ngotransactionToTransfer.getState().getData();

            // Stage 2. This flow can only be initiated by the current recipient.
            final AbstractParty lenderIdentity = getLenderIdentity(inputNGOTransaction);

            // Stage 3. Abort if the borrower started this flow.
            if (!getOurIdentity().equals(lenderIdentity)) {
                throw new IllegalStateException("NGOTransaction transfer can only be initiated by the lender.");
            }

            // Stage 4. Create the new ngotransaction state reflecting a new lender.
            progressTracker.setCurrentStep(BUILDING);
            final NGOTransaction transferredNGOTransaction = createOutputNGOTransaction(inputNGOTransaction);

            // Stage 4. Create the transfer command.
            final List<PublicKey> signerKeys = new ImmutableList.Builder<PublicKey>()
                    .addAll(inputNGOTransaction.getParticipantKeys())
                    .add(transferredNGOTransaction.getLender().getOwningKey()).build();
            final Command transferCommand = new Command<>(new NGOSecureContract.Commands.Transfer(), signerKeys);

            // Stage 5. Create a transaction builder, then add the states and commands.
            final TransactionBuilder builder = new TransactionBuilder(getFirstNotary())
                    .addInputState(ngotransactionToTransfer)
                    .addOutputState(transferredNGOTransaction, NGOSecureContract.NGO_TXN_CONTRACT_ID)
                    .addCommand(transferCommand);

            // Stage 6. Verify and sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder, inputNGOTransaction.getLender().getOwningKey());

            // Stage 7. Get a Party object for the borrower.
            progressTracker.setCurrentStep(SYNCING);
            final Party borrower = getBorrowerIdentity(inputNGOTransaction);

            // Stage 8. Send any keys and certificates so the signers can verify each other's identity.
            // We call `toSet` in case the borrower and the new lender are the same party.
            Set<FlowSession> sessions = new HashSet<>();
            Set<Party> parties = ImmutableSet.of(borrower, newLender);
            for (Party party : parties) {
                sessions.add(initiateFlow(party));
            }
            subFlow(new IdentitySyncFlow.Send(sessions, ptx.getTx(), SYNCING.childProgressTracker()));

            // Stage 9. Collect signatures from the borrower and the new lender.
            progressTracker.setCurrentStep(COLLECTING);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    sessions,
                    ImmutableList.of(inputNGOTransaction.getLender().getOwningKey()),
                    COLLECTING.childProgressTracker()));

            // Stage 10. Notarise and record, the transaction in our vaults. Send a copy to me as well.
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, ImmutableSet.of(getOurIdentity())));
        }

        @Suspendable
        private AbstractParty getLenderIdentity(NGOTransaction inputNGOTransaction) {
            if (inputNGOTransaction.getLender() instanceof AnonymousParty) {
                return resolveIdentity(inputNGOTransaction.getLender());
            } else {
                return inputNGOTransaction.getLender();
            }
        }

        @Suspendable
        private NGOTransaction createOutputNGOTransaction(NGOTransaction inputNGOTransaction) throws FlowException {
            if (anonymous) {
                // TODO: Is there a flow to get a key and cert only from the counterparty?
                final HashMap<Party, AnonymousParty> txKeys = subFlow(new SwapIdentitiesFlow(newLender));
                if (!txKeys.containsKey(newLender)) {
                    throw new FlowException("Couldn't get lender's conf. identity.");
                }
                final AnonymousParty anonymousLender = txKeys.get(newLender);
                return inputNGOTransaction.withNewLender(anonymousLender);
            } else {
                return inputNGOTransaction.withNewLender(newLender);
            }
        }

        @Suspendable
        private Party getBorrowerIdentity(NGOTransaction inputNGOTransaction) {
            if (inputNGOTransaction.getBorrower() instanceof AnonymousParty) {
                return resolveIdentity(inputNGOTransaction.getBorrower());
            } else {
                return (Party) inputNGOTransaction.getBorrower();
            }
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