package ngosecure.flows;

import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.StartedMockNode;
import ngosecure.vo.NGOTransaction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Currency;
import java.util.List;

import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.Currencies.POUNDS;
import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class SettleTransactionTests extends NGOTransactionTests {

    // Helper for extracting the cash output owned by a the node.
    private Cash.State getCashOutputByOwner(
            List<Cash.State> cashStates,
            StartedMockNode node) {
        Cash.State ownersCashState = null;
        for (Cash.State cashState : cashStates) {
            Party cashOwner = node.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(cashState.getOwner());
            if (cashOwner == chooseIdentity(node.getInfo())) {
                ownersCashState = cashState;
                break;
            }
        }
        return ownersCashState;
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void settleFlowCanOnlyBeStartedByBorrower() throws Exception {
        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        exception.expectCause(instanceOf(FlowException.class));
        settleNGOTransaction(issuedNGOTransaction.getLinearId(), b, POUNDS(1000), true);
    }

    @Test
    public void settleFlowFailsWhenBorrowerHasNoCash() throws Exception {
        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        exception.expectCause(instanceOf(FlowException.class));
        settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, POUNDS(1000), true);
    }

    @Test
    public void settleFlowFailsWhenBorrowerPledgesTooMuchCashToSettle() throws Exception {
        // Self issue cash.
        selfIssueCash(a, POUNDS(1500));
        network.waitQuiescent();

        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        exception.expectCause(instanceOf(FlowException.class));
        settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, POUNDS(1500), true);
    }

    @Test
    public void fullySettleNonAnonymousNGOTransaction() throws Exception {
        // Self issue cash.
        selfIssueCash(a, POUNDS(1500));
        network.waitQuiescent();

        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        SignedTransaction settleTransaction = settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, POUNDS(1000), true);
        network.waitQuiescent();
        assert(settleTransaction.getTx().outputsOfType(NGOTransaction.class).isEmpty());

        // Check both parties have the transaction.
        SignedTransaction aTx = a.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        assertEquals(aTx, bTx);
    }

    @Test
    public void fullySettleAnonymousNGOTransaction() throws Exception {
        // Self issue cash.
        selfIssueCash(a, POUNDS(1500));

        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), true);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        SignedTransaction settleTransaction = settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, POUNDS(1000), true);
        network.waitQuiescent();
        assert(settleTransaction.getTx().outputsOfType(NGOTransaction.class).isEmpty());

        // Check both parties have the transaction.
        SignedTransaction aTx = a.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        assertEquals(aTx, bTx);
    }

    @Test
    public void partiallySettleNonAnonymousNGOTransactionWithNonAnonymousCashPayment() throws Exception {
        // Self issue cash.
        selfIssueCash(a, POUNDS(1500));
        network.waitQuiescent();

        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        Amount<Currency> amountToSettle = POUNDS(500);
        SignedTransaction settleTransaction = settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, amountToSettle, false);
        network.waitQuiescent();
        assertEquals(1, settleTransaction.getTx().outputsOfType(NGOTransaction.class).size());

        // Check both parties have the transaction.
        SignedTransaction aTx = a.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        assertEquals(aTx, bTx);

        // Check the NGOTransaction paid amount is correctly updated.
        NGOTransaction partiallySettledNGOTransaction = settleTransaction.getTx().outputsOfType(NGOTransaction.class).get(0);
        assertEquals(amountToSettle, partiallySettledNGOTransaction.getPaid());

        // Check cash has gone to the correct parties.
        List<Cash.State> outputCash = settleTransaction.getTx().outputsOfType(Cash.State.class);
        assertEquals(2, outputCash.size());       // Cash to b and change to a.

        // Change addresses are always anonymous, I think.
        Cash.State change = getCashOutputByOwner(outputCash, a);
        assertEquals(POUNDS(1000), withoutIssuer(change.getAmount()));

        Cash.State payment = getCashOutputByOwner(outputCash, b);
        assertEquals(POUNDS(500), withoutIssuer(payment.getAmount()));
    }

    @Test
    public void partiallySettleNonAnonymousNGOTransactionWithAnonymousCashPayment() throws Exception {
        // Self issue cash.
        selfIssueCash(a, POUNDS(1500));
        network.waitQuiescent();

        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        Amount<Currency> amountToSettle = POUNDS(500);
        SignedTransaction settleTransaction = settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, amountToSettle, true);
        network.waitQuiescent();
        assertEquals(1, settleTransaction.getTx().outputsOfType(NGOTransaction.class).size());

        // Check both parties have the transaction.
        SignedTransaction aTx = a.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        assertEquals(aTx, bTx);

        // Check the NGOTransaction paid amount is correctly updated.
        NGOTransaction partiallySettledNGOTransaction = settleTransaction.getTx().outputsOfType(NGOTransaction.class).get(0);
        assertEquals(amountToSettle, partiallySettledNGOTransaction.getPaid());

        // Check cash has gone to the correct parties.
        List<Cash.State> outputCash = settleTransaction.getTx().outputsOfType(Cash.State.class);
        assertEquals(2, outputCash.size());       // Cash to b and change to a.

        Cash.State change = getCashOutputByOwner(outputCash, a);
        assertEquals(POUNDS(1000), withoutIssuer(change.getAmount()));

        Cash.State payment = getCashOutputByOwner(outputCash, b);
        assertEquals(POUNDS(500), withoutIssuer(payment.getAmount()));
    }

    @Test
    public void partiallySettleAnonymousNGOTransactionWithAnonymousCashPayment() throws Exception {
        // Self issue cash.
        selfIssueCash(a, POUNDS(1500));
        network.waitQuiescent();

        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), true);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Attempt settlement.
        Amount<Currency> amountToSettle = POUNDS(500);
        SignedTransaction settleTransaction = settleNGOTransaction(issuedNGOTransaction.getLinearId(), a, amountToSettle, true);
        network.waitQuiescent();
        assertEquals(1, settleTransaction.getTx().outputsOfType(NGOTransaction.class).size());

        // Check both parties have the transaction.
        SignedTransaction aTx = a.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        SignedTransaction bTx = b.getServices().getValidatedTransactions().getTransaction(settleTransaction.getId());
        assertEquals(aTx, bTx);

        // Check the NGOTransaction paid amount is correctly updated.
        NGOTransaction partiallySettledNGOTransaction = settleTransaction.getTx().outputsOfType(NGOTransaction.class).get(0);
        assertEquals(amountToSettle, partiallySettledNGOTransaction.getPaid());

        // Check cash has gone to the correct parties.
        List<Cash.State> outputCash = settleTransaction.getTx().outputsOfType(Cash.State.class);
        assertEquals(2, outputCash.size());       // Cash to b and change to a.

        Cash.State change = getCashOutputByOwner(outputCash, a);
        assertEquals(POUNDS(1000), withoutIssuer(change.getAmount()));

        Cash.State payment = getCashOutputByOwner(outputCash, b);
        assertEquals(POUNDS(500), withoutIssuer(payment.getAmount()));
    }
}