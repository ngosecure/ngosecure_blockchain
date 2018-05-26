package ngosecure.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(value = {"genericType", "intrinsicType"})
public class NGOInsightReport {

    @JsonProperty("AMOUNT")
    private Map<String, BigDecimal> AMOUNT;
    @JsonProperty("ORGANIZATION")
    private Map<String, String> ORGANIZATION;
    @JsonProperty("PREDICTION")
    private Map<String, String> PREDICTION;

    @JsonProperty("AMOUNT")
    public Map<String, BigDecimal> getAMOUNT() {
        return AMOUNT;
    }

    @JsonProperty("AMOUNT")
    public void setAMOUNT(Map<String, BigDecimal> AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    @JsonProperty("ORGANIZATION")
    public Map<String, String> getORGANIZATION() {
        return ORGANIZATION;
    }

    @JsonProperty("ORGANIZATION")
    public void setORGANIZATION(Map<String, String> ORGANIZATION) {
        this.ORGANIZATION = ORGANIZATION;
    }

    @JsonProperty("PREDICTION")
    public Map<String, String> getPREDICTION() {
        return PREDICTION;
    }

    @JsonProperty("PREDICTION")
    public void setPREDICTION(Map<String, String> PREDICTION) {
        this.PREDICTION = PREDICTION;
    }


}
