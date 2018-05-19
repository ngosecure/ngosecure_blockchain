package ngosecure.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import ngosecure.vo.NGOTransaction;

import java.util.List;

import static net.corda.finance.Currencies.DOLLARS;
import static net.corda.finance.Currencies.POUNDS;

/**
 * A base class to reduce the boilerplate when writing NGOTransaction contract tests.
 */
abstract class NGOSecureContractUnitTests {
    protected MockServices ledgerServices = new MockServices(
            ImmutableList.of("ngosecure", "net.corda.testing.contracts"));
    protected TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    protected TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    protected TestIdentity charlie = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    protected NGOTransaction oneDollarNGOTransaction = new NGOTransaction(POUNDS(1), alice.getParty(), bob.getParty());
    protected NGOTransaction tenDollarNGOTransaction = new NGOTransaction(DOLLARS(10), alice.getParty(), bob.getParty());
}

class DummyState implements ContractState {
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }
}

class DummyCommand implements CommandData {}