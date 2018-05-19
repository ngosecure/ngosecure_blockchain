package ngosecure.flows;

import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import ngosecure.vo.NGOTransaction;
import org.junit.Test;

import static net.corda.finance.Currencies.POUNDS;
import static net.corda.testing.internal.InternalTestUtilsKt.chooseIdentity;
import static org.junit.Assert.assertEquals;

public class IssueNGOTransactionTests extends NGOTransactionTests {

    @Test
    public void issueNonAnonymousNGOTransactionSuccessfully() throws Exception {
        SignedTransaction stx = issueNGOTransaction(a, b, POUNDS(1000), false);

        network.waitQuiescent();

        NGOTransaction aNGOTransaction = (NGOTransaction) a.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();
        NGOTransaction bNGOTransaction = (NGOTransaction) b.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();

        assertEquals(aNGOTransaction, bNGOTransaction);
    }

    @Test
    public void issueAnonymousNGOTransactionSuccessfully() throws Exception {
        SignedTransaction stx = issueNGOTransaction(a, b, POUNDS(1000), true);

        Party aIdentity = chooseIdentity(a.getServices().getMyInfo());
        Party bIdentity = chooseIdentity(b.getServices().getMyInfo());

        network.waitQuiescent();

        NGOTransaction aNGOTransaction = (NGOTransaction) a.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();
        NGOTransaction bNGOTransaction = (NGOTransaction) b.getServices().loadState(stx.getTx().outRef(0).getRef()).getData();

        assertEquals(aNGOTransaction, bNGOTransaction);

        Party maybePartyALookedUpByA = a.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aNGOTransaction.getBorrower());
        Party maybePartyALookedUpByB = b.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aNGOTransaction.getBorrower());

        assertEquals(aIdentity, maybePartyALookedUpByA);
        assertEquals(aIdentity, maybePartyALookedUpByB);

        Party maybePartyCLookedUpByA = a.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aNGOTransaction.getLender());
        Party maybePartyCLookedUpByB = b.getServices().getIdentityService().requireWellKnownPartyFromAnonymous(aNGOTransaction.getLender());

        assertEquals(bIdentity, maybePartyCLookedUpByA);
        assertEquals(bIdentity, maybePartyCLookedUpByB);
    }
}
