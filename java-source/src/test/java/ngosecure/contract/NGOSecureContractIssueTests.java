package ngosecure.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import ngosecure.vo.NGOTransaction;
import org.junit.Test;

import static net.corda.finance.Currencies.*;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static ngosecure.contract.NGOSecureContract.NGO_TXN_CONTRACT_ID;

public class NGOSecureContractIssueTests extends NGOSecureContractUnitTests {

    @Test
    public void issueNGOTransactionTransactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, new DummyState());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("No inputs should be consumed when issuing an NGOTransaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.verifies(); // As there are no input states.
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOnlyOneOutputNGOTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction); // Two outputs fails.
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("Only one NGOTransaction state should be created when issuing an NGOTransaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction); // One output passes.
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotIssueZeroValueNGOTransactions() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(POUNDS(0), alice.getParty(), bob.getParty())); // Zero amount fails.
                tx.failsWith("A newly issued NGOTransaction must have a positive amount.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(SWISS_FRANCS(100), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(POUNDS(1), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(DOLLARS(10), alice.getParty(), bob.getParty()));
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderAndBorrowerMustSignIssueNGOTransactionTransaction() {
        TestIdentity dummyIdentity = new TestIdentity(new CordaX500Name("Dummy", "", "GB"));

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(dummyIdentity.getPublicKey(), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("Both lender and borrower together only may sign NGOTransaction issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(alice.getPublicKey(), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("Both lender and borrower together only may sign NGOTransaction issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(bob.getPublicKey(), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("Both lender and borrower together only may sign NGOTransaction issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bob.getPublicKey(), bob.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("Both lender and borrower together only may sign NGOTransaction issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bob.getPublicKey(), bob.getPublicKey(), dummyIdentity.getPublicKey(), alice.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.failsWith("Both lender and borrower together only may sign NGOTransaction issue transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bob.getPublicKey(), bob.getPublicKey(), bob.getPublicKey(), alice.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderAndBorrowerCannotBeTheSame() {
        NGOTransaction borrowerIsLenderNGOTransaction = new NGOTransaction(POUNDS(10), alice.getParty(), alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, borrowerIsLenderNGOTransaction);
                tx.failsWith("The lender and borrower cannot be the same identity.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Issue());
                tx.output(NGO_TXN_CONTRACT_ID, oneDollarNGOTransaction);
                tx.verifies();
                return null;
            });
            return null;
        }));
    }
}