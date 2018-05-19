package ngosecure.vo;

public class NGOTransactionReport {
    private String COUNTRY;
    private String CITY;
    private String NOTARY;
    private String AMOUNT;
    private String ORGANIZATION;
    private String DONOR;
    private String TXN_TYPE;
    private String TIMESTAMP;

    public String getCOUNTRY() {
        return COUNTRY;
    }

    public void setCOUNTRY(String COUNTRY) {
        this.COUNTRY = COUNTRY;
    }

    public String getCITY() {
        return CITY;
    }

    public void setCITY(String CITY) {
        this.CITY = CITY;
    }

    public String getNOTARY() {
        return NOTARY;
    }

    public void setNOTARY(String NOTARY) {
        this.NOTARY = NOTARY;
    }

    public String getAMOUNT() {
        return AMOUNT;
    }

    public void setAMOUNT(String AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    public String getORGANIZATION() {
        return ORGANIZATION;
    }

    public void setORGANIZATION(String ORGANIZATION) {
        this.ORGANIZATION = ORGANIZATION;
    }

    public String getTXN_TYPE() {
        return TXN_TYPE;
    }

    public void setTXN_TYPE(String TXN_TYPE) {
        this.TXN_TYPE = TXN_TYPE;
    }

    public String getTIMESTAMP() {
        return TIMESTAMP;
    }

    public void setTIMESTAMP(String TIMESTAMP) {
        this.TIMESTAMP = TIMESTAMP;
    }

    public String getDONOR() {
        return DONOR;
    }

    public void setDONOR(String DONOR) {
        this.DONOR = DONOR;
    }

    public NGOTransactionReport(){

    }

    public NGOTransactionReport(String country,String city,String notary,
                                String amount,String org,String txnType,String timeStamp,String donor){
        this.COUNTRY = country;
        this.CITY = city;
        this.NOTARY = notary;
        this.AMOUNT = amount;
        this.ORGANIZATION = org;
        this.TXN_TYPE = txnType;
        this.TIMESTAMP = timeStamp;
        this.DONOR = donor;
    }

}
