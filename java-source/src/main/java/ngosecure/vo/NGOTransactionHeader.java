package ngosecure.vo;

public class NGOTransactionHeader {

    private String COUNTRY;
    private String CITY;
    private String NOTARY;
    private String AMOUNT;
    private String ORGANIZATION;
    private String TXN_TYPE;
    private String TIMESTAMP;
    private String DONOR;


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

    public NGOTransactionHeader(){
        this.COUNTRY = "COUNTRY";
        this.CITY = "CITY";
        this.NOTARY = "NOTARY";
        this.AMOUNT = "AMOUNT";
        this.ORGANIZATION = "ORGANIZATION";
        this.TXN_TYPE = "TXN_TYPE";
        this.TIMESTAMP = "TIMESTAMP";
        this.DONOR = "DONOR";
    }
}
