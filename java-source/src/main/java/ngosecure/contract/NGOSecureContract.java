package ngosecure.contract;

import com.google.common.collect.Sets;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;
import ngosecure.vo.NGOTransaction;

import java.security.PublicKey;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.utils.StateSumming.sumCash;

public class NGOSecureContract implements Contract {
    public static final String NGO_TXN_CONTRACT_ID = "ngosecure.contract.NGOSecureContract";

    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands {
        }

        class Transfer extends TypeOnlyCommandData implements Commands {
        }

        class Settle extends TypeOnlyCommandData implements Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx, setOfSigners);
        } else if (commandData instanceof Commands.Transfer) {
            verifyTransfer(tx, setOfSigners);
        } else if (commandData instanceof Commands.Settle) {
            verifySettle(tx, setOfSigners);
        } else {
            throw new IllegalArgumentException("Unrecognised command.");
        }
    }

    private Set<PublicKey> keysFromParticipants(NGOTransaction transaction) {
        return transaction
                .getParticipants().stream()
                .map(AbstractParty::getOwningKey)
                .collect(toSet());
    }

    // This only allows one transaction issuance per transaction.
    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            req.using("No inputs should be consumed when issuing an transaction.",
                    tx.getInputStates().isEmpty());
            req.using("Only one transaction state should be created when issuing an transaction.", tx.getOutputStates().size() == 1);
            NGOTransaction transaction = (NGOTransaction) tx.getOutputStates().get(0);
            req.using("A newly issued transaction must have a positive amount.", transaction.getAmount().getQuantity() > 0);
            req.using("The lender and borrower cannot be the same identity.", !transaction.getBorrower().equals(transaction.getLender()));
            req.using("Both lender and borrower together only may sign transaction issue transaction.",
                    signers.equals(keysFromParticipants(transaction)));
            return null;
        });
    }

    // This only allows one transaction transfer per transaction.
    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            req.using("An transaction transfer transaction should only consume one input state.", tx.getInputs().size() == 1);
            req.using("An transaction transfer transaction should only create one output state.", tx.getOutputs().size() == 1);
            NGOTransaction input = tx.inputsOfType(NGOTransaction.class).get(0);
            NGOTransaction output = tx.outputsOfType(NGOTransaction.class).get(0);
            req.using("Only the lender property may change.", input.withoutLender().equals(output.withoutLender()));
            req.using("The lender property must change in a transfer.", !input.getLender().equals(output.getLender()));
            req.using("The borrower, old lender and new lender only must sign an transaction transfer transaction",
                    signers.equals(Sets.union(keysFromParticipants(input), keysFromParticipants(output))));
            return null;
        });
    }

    private void verifySettle(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            // Check for the presence of an input transaction state.
            List<NGOTransaction> transactionInputs = tx.inputsOfType(NGOTransaction.class);
            req.using("There must be one input transaction.", transactionInputs.size() == 1);

            // Check there are output cash states.
            // We don't care about cash inputs, the Cash contract handles those.
            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
            req.using("There must be output cash.", !cash.isEmpty());

            // Check that the cash is being assigned to us.
            NGOTransaction inputNGOTransaction = transactionInputs.get(0);
            List<Cash.State> acceptableCash = cash.stream().filter(it -> it.getOwner().equals(inputNGOTransaction.getLender())).collect(Collectors.toList());
            req.using("There must be output cash paid to the recipient.", !acceptableCash.isEmpty());

            // Sum the cash being sent to us (we don't care about the issuer).
            Amount<Currency> sumAcceptableCash = withoutIssuer(sumCash(acceptableCash));
            Amount<Currency> amountOutstanding = inputNGOTransaction.getAmount().minus(inputNGOTransaction.getPaid());
            req.using("The amount settled cannot be more than the amount outstanding.", amountOutstanding.compareTo(sumAcceptableCash) >= 0);

            List<NGOTransaction> transactionOutputs = tx.outputsOfType(NGOTransaction.class);

            // Check to see if we need an output transaction or not.
            if (amountOutstanding.equals(sumAcceptableCash)) {
                // If the transaction has been fully settled then there should be no transaction output state.
                req.using("There must be no output transaction as it has been fully settled.", transactionOutputs.isEmpty());
            } else {
                // If the transaction has been partially settled then it should still exist.
                req.using("There must be one output transaction.", transactionOutputs.size() == 1);

                // Check only the paid property changes.
                NGOTransaction outputNGOTransaction = transactionOutputs.get(0);
                req.using("The amount may not change when settling.", inputNGOTransaction.getAmount().equals(outputNGOTransaction.getAmount()));
                req.using("The borrower may not change when settling.", inputNGOTransaction.getBorrower().equals(outputNGOTransaction.getBorrower()));
                req.using("The lender may not change when settling.", inputNGOTransaction.getLender().equals(outputNGOTransaction.getLender()));
                req.using("The linearId may not change when settling.", inputNGOTransaction.getLinearId().equals(outputNGOTransaction.getLinearId()));

                // Check the paid property is updated correctly.
                req.using("Paid property incorrectly updated.", outputNGOTransaction.getPaid().equals(inputNGOTransaction.getPaid().plus(sumAcceptableCash)));
            }

            // Checks the required parties have signed.
            req.using("Both lender and borrower together only must sign transaction settle transaction.", signers.equals(keysFromParticipants(inputNGOTransaction)));
            return null;
        });
    }
}