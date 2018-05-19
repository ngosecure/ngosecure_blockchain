package ngosecure.vo;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import ngosecure.constants.NGOConstants;
import java.security.PublicKey;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import static net.corda.core.utilities.EncodingUtils.toBase58String;

public class NGOTransaction implements LinearState {
    private final Amount<Currency> amount;
    private final AbstractParty lender;
    private final AbstractParty borrower;
    private final Amount<Currency> paid;
    private final UniqueIdentifier linearId;
    private String amountLentVal;
    private String amountPaidVal;
    private String lenderName;
    private String borrowerName;

    @ConstructorForDeserialization
    public NGOTransaction(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower, Amount<Currency> paid, UniqueIdentifier linearId) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
        this.amountLentVal = String.valueOf(amount.toDecimal()) + " " + NGOConstants.NGO_CURRENCY;
        this.amountPaidVal = String.valueOf(paid.toDecimal()) + " " + NGOConstants.NGO_CURRENCY;
        if(lender.nameOrNull() !=  null){
            this.lenderName = lender.nameOrNull().getOrganisation();
        }
        if(borrower.nameOrNull() !=  null){
            this.borrowerName = borrower.nameOrNull().getOrganisation();
        }
    }

    public NGOTransaction(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower, Amount<Currency> paid) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = new UniqueIdentifier();
    }

    public NGOTransaction(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = new Amount<>(0, amount.getToken());
        this.linearId = new UniqueIdentifier();
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public AbstractParty getLender() {
        return lender;
    }

    public AbstractParty getBorrower() {
        return borrower;
    }

    public Amount<Currency> getPaid() {
        return paid;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    public NGOTransaction pay(Amount<Currency> amountToPay) {
        return new NGOTransaction(
                this.amount,
                this.lender,
                this.borrower,
                this.paid.plus(amountToPay),
                this.linearId
        );
    }

    public NGOTransaction withNewLender(AbstractParty newLender) {
        return new NGOTransaction(this.amount, newLender, this.borrower, this.paid, this.linearId);
    }

    public NGOTransaction withoutLender() {
        return new NGOTransaction(this.amount, NullKeys.INSTANCE.getNULL_PARTY(), this.borrower, this.paid, this.linearId);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String lenderString;
        if (this.lender instanceof Party) {
            lenderString = ((Party) lender).getName().getOrganisation();
        } else {
            PublicKey lenderKey = this.lender.getOwningKey();
            lenderString = toBase58String(lenderKey);
        }

        String borrowerString;
        if (this.borrower instanceof Party) {
            borrowerString = ((Party) borrower).getName().getOrganisation();
        } else {
            PublicKey borrowerKey = this.borrower.getOwningKey();
            borrowerString = toBase58String(borrowerKey);
        }

        return String.format("NGOTransaction(%s): %s owes %s %s and has paid %s so far.",
                this.linearId, borrowerString, lenderString, this.amount, this.paid);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NGOTransaction)) {
            return false;
        }
        NGOTransaction other = (NGOTransaction) obj;
        return amount.equals(other.getAmount())
                && lender.equals(other.getLender())
                && borrower.equals(other.getBorrower())
                && paid.equals(other.getPaid())
                && linearId.equals(other.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, lender, borrower, paid, linearId);
    }

    public String getAmountLentVal() {
        return amountLentVal;
    }

    public void setAmountLentVal(String amountLentVal) {
        this.amountLentVal = amountLentVal;
    }

    public String getAmountPaidVal() {
        return amountPaidVal;
    }

    public void setAmountPaidVal(String amountPaidVal) {
        this.amountPaidVal = amountPaidVal;
    }

    public String getLenderName() {
        return lenderName;
    }

    public void setLenderName(String lenderName) {
        this.lenderName = lenderName;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }
}