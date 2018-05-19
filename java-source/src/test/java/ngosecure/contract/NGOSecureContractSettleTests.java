package ngosecure.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.core.TestIdentity;
import ngosecure.vo.NGOTransaction;
import org.junit.Test;

import java.util.Currency;

import static ngosecure.contract.NGOSecureContract.NGO_TXN_CONTRACT_ID;
import static net.corda.finance.Currencies.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class NGOSecureContractSettleTests extends NGOSecureContractUnitTests {
    private TestIdentity issuer = new TestIdentity(new CordaX500Name("MegaBank", "", "US"));
    private Byte defaultRef = Byte.MAX_VALUE;
    private PartyAndReference defaultIssuer = issuer.ref(defaultRef);

    private Cash.State createCashState(Amount<Currency> amount, AbstractParty owner) {
        return new Cash.State(issuedBy(amount, defaultIssuer), owner);
    }

    @Test
    public void mustIncludeSettleCommand() {
        Cash.State inputCash = createCashState(DOLLARS(5), bob.getParty());
        Cash.State outputCash = (Cash.State) inputCash.withNewOwner(alice.getParty()).getOwnableState();
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.input(NGO_TXN_CONTRACT_ID, inputCash);
                tx.output(NGO_TXN_CONTRACT_ID, outputCash);
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.fails();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.input(NGO_TXN_CONTRACT_ID, inputCash);
                tx.output(NGO_TXN_CONTRACT_ID, outputCash);
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new DummyCommand()); // Wrong type.
                tx.fails();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.input(NGO_TXN_CONTRACT_ID, inputCash);
                tx.output(NGO_TXN_CONTRACT_ID, outputCash);
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle()); // Correct Type.
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustHaveOnlyOneInputNGOTransaction() {
        NGOTransaction duplicateNGOTransaction = new NGOTransaction(DOLLARS(10), alice.getParty(), bob.getParty());
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.failsWith("There must be one input NGOTransaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, duplicateNGOTransaction);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.failsWith("There must be one input NGOTransaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.input(NGO_TXN_CONTRACT_ID, tenDollars);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustBeCashOutputStatesPresent() {
        Cash.State cash = createCashState(DOLLARS(5), bob.getParty());
        CommandAndState cashPayment = cash.withNewOwner(alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("There must be output cash.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, cash);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.output(NGO_TXN_CONTRACT_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustBeCashOutputStatesWithRecipientAsOwner() {
        Cash.State cash = createCashState(DOLLARS(5), bob.getParty());
        CommandAndState invalidCashPayment = cash.withNewOwner(charlie.getParty());
        CommandAndState validCashPayment = cash.withNewOwner(alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, cash);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.output(NGO_TXN_CONTRACT_ID, invalidCashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), invalidCashPayment.getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("There must be output cash paid to the recipient.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, cash);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.output(NGO_TXN_CONTRACT_ID, validCashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), validCashPayment.getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cashSettlementAmountMustBeLessThanTheRemainingAmount() {
        Cash.State elevenDollars = createCashState(DOLLARS(11), bob.getParty());
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, elevenDollars);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(11)));
                tx.output(NGO_TXN_CONTRACT_ID, elevenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), elevenDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("The amount settled cannot be more than the amount outstanding.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollars);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cashSettlementMustBeInTheCorrectCurrency() {
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State tenPounds = createCashState(POUNDS(10), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, tenPounds);
                tx.output(NGO_TXN_CONTRACT_ID, tenPounds.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenPounds.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("Token mismatch: GBP vs USD");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollars);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(alice.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustHaveOutputNGOTransactionIfNotFullySettling() {
        Cash.State tenDollars = createCashState(DOLLARS(10), bob.getParty());
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("There must be one output NGOTransaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollars);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(10)));
                tx.output(NGO_TXN_CONTRACT_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("There must be no output NGOTransaction as it has been fully settled.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollars);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, tenDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.command(bob.getPublicKey(), tenDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void onlyPaidPropertyMayChange() {
        Cash.State fiveDollars = createCashState(DOLLARS(5), bob.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(tenDollarNGOTransaction.getAmount(), tenDollarNGOTransaction.getLender(), alice.getParty(), DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("The borrower may not change when settling.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(DOLLARS(0), tenDollarNGOTransaction.getLender(), tenDollarNGOTransaction.getBorrower(), DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("The amount may not change when settling.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(NGO_TXN_CONTRACT_ID, new NGOTransaction(DOLLARS(10), charlie.getParty(), tenDollarNGOTransaction.getBorrower(), DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("The lender may not change when settling.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.input(NGO_TXN_CONTRACT_ID, fiveDollars);
                tx.output(NGO_TXN_CONTRACT_ID, fiveDollars.withNewOwner(alice.getParty()).getOwnableState());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.command(bob.getPublicKey(), fiveDollars.withNewOwner(bob.getParty()).getCommand());
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void mustBeSignedByAllParticipants() {
        Cash.State cash = createCashState(DOLLARS(5), bob.getParty());
        CommandAndState cashPayment = cash.withNewOwner(alice.getParty());
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, cash);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.command(ImmutableList.of(alice.getPublicKey(), charlie.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign NGOTransaction settle transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, cash);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.command(bob.getPublicKey(), new NGOSecureContract.Commands.Settle());
                tx.failsWith("Both lender and borrower together only must sign NGOTransaction settle transaction.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(NGO_TXN_CONTRACT_ID, cash);
                tx.input(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction);
                tx.output(NGO_TXN_CONTRACT_ID, cashPayment.getOwnableState());
                tx.command(bob.getPublicKey(), cashPayment.getCommand());
                tx.output(NGO_TXN_CONTRACT_ID, tenDollarNGOTransaction.pay(DOLLARS(5)));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new NGOSecureContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }
}