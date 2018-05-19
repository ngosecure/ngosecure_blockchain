package ngosecure.flows;

import net.corda.core.transactions.SignedTransaction;
import ngosecure.vo.NGOTransaction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.assertEquals;
import static net.corda.finance.Currencies.POUNDS;
import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;
import static org.hamcrest.CoreMatchers.instanceOf;

public class TransferNGOTransactionTests extends NGOTransactionTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void transferNonAnonymousNGOTransactionSuccessfully() throws Exception {
        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Transfer NGOTransaction.
        SignedTransaction transferTransaction = transferNGOTransaction(issuedNGOTransaction.getLinearId(), b, c, false);
        network.waitQuiescent();
        NGOTransaction transferredNGOTransaction = (NGOTransaction) transferTransaction.getTx().getOutputStates().get(0);

        // Check the issued NGOTransaction with the new lender is the transferred NGOTransaction
        assertEquals(issuedNGOTransaction.withNewLender(chooseIdentity(c.getInfo())), transferredNGOTransaction);

        // Check everyone has the transfer transaction.
        NGOTransaction aNGOTransaction = (NGOTransaction) a.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        NGOTransaction bNGOTransaction = (NGOTransaction) b.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        NGOTransaction cNGOTransaction = (NGOTransaction) c.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        assertEquals(aNGOTransaction, bNGOTransaction);
        assertEquals(bNGOTransaction, cNGOTransaction);
    }

    @Test
    public void transferAnonymousNGOTransactionSuccessfully() throws Exception {
        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Transfer NGOTransaction.
        SignedTransaction transferTransaction = transferNGOTransaction(issuedNGOTransaction.getLinearId(), b, c, true);
        network.waitQuiescent();
        NGOTransaction transferredNGOTransaction = (NGOTransaction) transferTransaction.getTx().getOutputStates().get(0);

        // Check the issued NGOTransaction with the new lender is the transferred NGOTransaction.
        assertEquals(issuedNGOTransaction.withNewLender(transferredNGOTransaction.getLender()), transferredNGOTransaction);

        // Check everyone has the transfer transaction.
        NGOTransaction aNGOTransaction = (NGOTransaction) a.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        NGOTransaction bNGOTransaction = (NGOTransaction) b.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        NGOTransaction cNGOTransaction = (NGOTransaction) c.getServices().loadState(transferTransaction.getTx().outRef(0).getRef()).getData();
        assertEquals(aNGOTransaction, bNGOTransaction);
        assertEquals(bNGOTransaction, cNGOTransaction);
    }

    @Test
    public void transferFlowCanOnlyBeStartedByLender() throws Exception {
        // Issue NGOTransaction.
        SignedTransaction issuanceTransaction = issueNGOTransaction(a, b, POUNDS(1000), false);
        network.waitQuiescent();
        NGOTransaction issuedNGOTransaction = (NGOTransaction) issuanceTransaction.getTx().getOutputStates().get(0);

        // Transfer NGOTransaction.
        exception.expectCause(instanceOf(IllegalStateException.class));
        transferNGOTransaction(issuedNGOTransaction.getLinearId(), a, c, false);
    }
}
