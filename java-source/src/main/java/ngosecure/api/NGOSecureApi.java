package ngosecure.api;

import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import ngosecure.constants.NGOConstants;
import ngosecure.enums.NGOTransactionType;
import ngosecure.flows.NGOTransactionIssuance;
import ngosecure.flows.NGOTransactionSettlement;
import ngosecure.flows.NGOTransactionTransfer;
import ngosecure.util.NGOSecureUtil;
import ngosecure.vo.NGOTransaction;
import ngosecure.vo.NGOTransactionReport;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.stream.Collectors.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static net.corda.finance.contracts.GetBalances.getCashBalances;

@Path("ngosecure")
public class NGOSecureApi {
    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    public NGOSecureApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
    }

    @GET
    @Path("isnotary")
    @Produces(MediaType.APPLICATION_JSON)
    public Boolean isnotary() {
        return rpcOps.notaryIdentities().get(0).getName().getOrganisation().
                equalsIgnoreCase(myIdentity.getName().getOrganisation());
    }

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Party> me() {
        return ImmutableMap.of("me", myIdentity);
    }

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> peers() {
        return ImmutableMap.of("peers", rpcOps.networkMapSnapshot()
                .stream()
                .filter(nodeInfo -> !nodeInfo.getLegalIdentities().get(0).equals(myIdentity))
                .map(it -> it.getLegalIdentities().get(0).getName().getOrganisation())
                .collect(toList()));
    }

    @GET
    @Path("owed-per-currency")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Currency, Long> owedPerCurrency() {
        return rpcOps.vaultQuery(NGOTransaction.class).getStates()
                .stream()
                .filter(it -> it.getState().getData().getLender() != myIdentity)
                .map(it -> it.getState().getData().getAmount())
                .collect(groupingBy(Amount::getToken, summingLong(Amount::getQuantity)));
    }

    @GET
    @Path("ngotransactions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NGOTransaction> ngotransactions() {
        List<StateAndRef<NGOTransaction>> statesAndRefs = rpcOps.vaultQuery(NGOTransaction.class).getStates();

        return statesAndRefs.stream()
                .map(stateAndRef -> stateAndRef.getState().getData())
                .map(state -> {
                    // We map the anonymous lender and borrower to well-known identities if possible.
                    AbstractParty possiblyWellKnownLender = rpcOps.wellKnownPartyFromAnonymous(state.getLender());
                    if (possiblyWellKnownLender == null) {
                        possiblyWellKnownLender = state.getLender();
                    }

                    AbstractParty possiblyWellKnownBorrower = rpcOps.wellKnownPartyFromAnonymous(state.getBorrower());
                    if (possiblyWellKnownBorrower == null) {
                        possiblyWellKnownBorrower = state.getBorrower();
                    }

                    return new NGOTransaction(
                            //new Amount<Currency>(100,Currency.getInstance(Locale.US)),
                            state.getAmount(),
                            possiblyWellKnownLender,
                            possiblyWellKnownBorrower,
                            state.getPaid(),
                            state.getLinearId());
                })
                .collect(toList());
    }

    @GET
    @Path("transactionledger")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NGOTransactionReport> transactionledger() {
        return new NGOSecureUtil().retrieveLedgerTransactions(myIdentity.getName().getOrganisation());
    }

    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<Cash.State>> cash() {
        return rpcOps.vaultQuery(Cash.State.class).getStates();
    }

    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON )
    public Map<String,String> cashBalances() {
        String cashDisplayVal = "";
        Amount<Currency> cashAmount = getCashBalances(rpcOps).get(Currency.getInstance("USD"));
        if(cashAmount != null){
            cashDisplayVal = String.valueOf(cashAmount.toDecimal()) + " " + NGOConstants.NGO_CURRENCY;
        }
        return  ImmutableMap.of("balance",cashDisplayVal);
    }
   /* public Map<Currency, Amount<Currency>> cashBalances() {
        return getCashBalances(rpcOps);
    }*/

    @GET
    @Path("self-issue-cash")
    public Response selfIssueCash(
            @QueryParam(value = "amount") int amount,
            @QueryParam(value = "currency") String currency) {

        // 1. Prepare issue request.
        final Amount<Currency> issueAmount = new Amount<>((long) amount * 100, Currency.getInstance("USD"));
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        final Party notary = notaries.get(0);
        final OpaqueBytes issueRef = OpaqueBytes.of(new byte[1]);
        final CashIssueFlow.IssueRequest issueRequest = new CashIssueFlow.IssueRequest(issueAmount, issueRef, notary);

        // 2. Start flow and wait for response.
        try {
            final FlowHandle<AbstractCashFlow.Result> flowHandle = rpcOps.startFlowDynamic(CashIssueFlow.class, issueRequest);
            final AbstractCashFlow.Result result = flowHandle.getReturnValue().get();
            String msg = result.getStx().getTx().getOutputStates().get(0).toString();

            NGOTransactionReport ngoTransactionReport = new NGOTransactionReport(this.myIdentity.getName().getCountry()
                    ,this.myIdentity.getName().getLocality(), notary.getName().getOrganisation(),
                    issueAmount.toDecimal().toString(),this.myIdentity.getName().getOrganisation(),
                    NGOTransactionType.SELF_ISSUANCE.name(),new Date().toString(),
                    this.myIdentity.getName().getOrganisation());
            new NGOSecureUtil().buildNGOTransactionReport(ngoTransactionReport);

            if(msg.contains("USD")){
                msg = msg.replaceAll("USD",NGOConstants.NGO_CURRENCY);
            }

            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("create-transaction")
    public Response issueNGOTransaction(
            @QueryParam(value = "amount") int amount,
            @QueryParam(value = "currency") String currency,
            @QueryParam(value = "party") String party) {

        // 1. Get party objects for the counterparty.
        final Set<Party> lenderIdentities = rpcOps.partiesFromName(party, false);
        if (lenderIdentities.size() != 1) {
            final String errMsg = String.format("Found %d identities for the lender.", lenderIdentities.size());
            throw new IllegalStateException(errMsg);
        }
        final Party lenderIdentity = lenderIdentities.iterator().next();

        // 2. Create an amount object.
        final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance("USD"));

        // 3. Start the IssueNGOTransaction flow. We block and wait for the flow to return.
        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    NGOTransactionIssuance.Initiator.class,
                    issueAmount, lenderIdentity, true
            );

            final SignedTransaction result = flowHandle.getReturnValue().get();
            String msg = String.format("Transaction id %s committed to ledger.\n%s",
                    result.getId(), result.getTx().getOutputStates().get(0));

            //Writing transaction output for Notary
            Party notary = rpcOps.notaryIdentities().get(0);
            CordaX500Name org = lenderIdentity.getName();

            NGOTransactionReport ngoTransactionReport = new NGOTransactionReport(org.getCountry(),org.getLocality(),
                    notary.getName().getOrganisation(),issueAmount.toDecimal().toString(),org.getOrganisation(),
                    NGOTransactionType.ISSUANCE.name(),new Date().toString(),this.myIdentity.getName().getOrganisation());
            new NGOSecureUtil().buildNGOTransactionReport(ngoTransactionReport);

            if(msg.contains("USD")){
                msg = msg.replaceAll("USD",NGOConstants.NGO_CURRENCY);
            }

            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("transfer-transaction")
    public Response transferNGOTransaction(
            @QueryParam(value = "id") String id,
            @QueryParam(value = "party") String party) {
        final UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(id);

        final Set<Party> newLenders = rpcOps.partiesFromName(party, false);
        if (newLenders.size() != 1) {
            final String errMsg = String.format("Found %d identities for the new lender.", newLenders.size());
            throw new IllegalStateException(errMsg);
        }
        final Party newLender = newLenders.iterator().next();

        try {
            final FlowHandle flowHandle = rpcOps.startFlowDynamic(
                    NGOTransactionTransfer.Initiator.class,
                    linearId, newLender, true);

            flowHandle.getReturnValue().get();
            final String msg = String.format("NGOTransaction %s transferred to %s.", id, party);
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("settle-transaction")
    public Response settleNGOTransaction(
            @QueryParam(value = "id") String id,
            @QueryParam(value = "amount") int amount,
            @QueryParam(value = "currency") String currency,
            @QueryParam(value = "donor") String donor) {
        UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(id);
        Amount<Currency> settleAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));

        try {
            final FlowHandle flowHandle = rpcOps.startFlowDynamic(
                    NGOTransactionSettlement.Initiator.class,
                    linearId, settleAmount, true);

            flowHandle.getReturnValue().get();
            final String msg = String.format("%s %s paid off on ngo transaction id %s.", amount,
                    NGOConstants.NGO_CURRENCY, id);

            //Writing transaction output for Notary
            final Party lenderIdentity = rpcOps.partiesFromName(donor, false).iterator().next();
            CordaX500Name org = lenderIdentity.getName();
            final Party notary = rpcOps.notaryIdentities().get(0);

            NGOTransactionReport ngoTransactionReport = new NGOTransactionReport(org.getCountry(),org.getLocality(),
                    notary.getName().getOrganisation(),settleAmount.toDecimal().toString(),org.getOrganisation(),
                    NGOTransactionType.SETTLEMENT.name(),new Date().toString(),
                    this.myIdentity.getName().getOrganisation());
            new NGOSecureUtil().buildNGOTransactionReport(ngoTransactionReport);

            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}