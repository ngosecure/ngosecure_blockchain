package ngosecure.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import ngosecure.constants.NGOConstants;
import ngosecure.vo.NGOInsightReport;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NGOInsightsProcessor {

    public static void main(String[] args) throws IOException, ParseException {

      /*  HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        HttpGet httpGet = new HttpGet(NGOConstants.NGO_INSIGHTS_ENDPOINT);

        CloseableHttpClient httpClient = httpClientBuilder.build();
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        String responseStr = EntityUtils.toString(httpResponse.getEntity());

        System.out.println("Response Code: " + httpResponse.getStatusLine().getStatusCode());
        System.out.println("Response Str: " + responseStr);

        JSONParser jsonParser = new JSONParser();
        Object object = jsonParser.parse(responseStr);
        System.out.println("Result Obj: " + object.toString());

        org.json.JSONObject jsonObject = new org.json.JSONObject(object.toString());

        System.out.println("jsonObject: " + jsonObject.toString());

        ObjectMapper mapper = new ObjectMapper();
        NGOInsightReport ngoInsightReport = mapper.readValue(jsonObject.toString(),NGOInsightReport.class);

        System.out.println("Amount: " + ngoInsightReport.getAMOUNT());
        System.out.println("Organization: " + ngoInsightReport.getORGANIZATION());
        System.out.println("Prediction: " + ngoInsightReport.getPREDICTION());

        //  System.out.println("InsightsMap: " + retrieveNGOInsights(ngoInsightReport));*/

    }

    /**
     * To call the NGO Secure insights machine learning engine
     * @return ngoInsight report
     * @throws IOException Generic IO ex
     * @throws ParseException Generic Parse ex
     */
    public NGOInsightReport retrieveNGOInsights() throws IOException, ParseException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        HttpGet httpGet = new HttpGet(NGOConstants.NGO_INSIGHTS_ENDPOINT);

        CloseableHttpClient httpClient = httpClientBuilder.build();
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        String responseStr = EntityUtils.toString(httpResponse.getEntity());

        System.out.println("Response Code: " + httpResponse.getStatusLine().getStatusCode());
        System.out.println("Response Str: " + responseStr);

        JSONParser jsonParser = new JSONParser();
        Object object = jsonParser.parse(responseStr);
        System.out.println("Result Obj: " + object.toString());

        org.json.JSONObject jsonObject = new org.json.JSONObject(object.toString());

        System.out.println("jsonObject: " + jsonObject.toString());

        ObjectMapper mapper = new ObjectMapper();
        NGOInsightReport ngoInsightReport = mapper.readValue(jsonObject.toString(),NGOInsightReport.class);

        System.out.println("Amount: " + ngoInsightReport.getAMOUNT());
        System.out.println("Organization: " + ngoInsightReport.getORGANIZATION());
        System.out.println("Prediction: " + ngoInsightReport.getPREDICTION());

        return ngoInsightReport;
    }

    /**
     * Method to build ngo insights map
     * @param ngoInsightReport ngoInsight report
     * @return insights map
     */
    public  HashMap<String,List<String>> buildInsightsMap(NGOInsightReport ngoInsightReport){
        HashMap<String,List<String>> insightsMap = new HashMap<>();
        List<String> partyList;

        Iterator<String > OrgIterator = ngoInsightReport.getORGANIZATION().keySet().iterator();
        while(OrgIterator.hasNext()){
            String orgKey = OrgIterator.next();
            String insightsKey = retrieveInsightKey(ngoInsightReport.getPREDICTION().get(orgKey));
            if(!insightsMap.containsKey(insightsKey)){
                partyList = new ArrayList<>();
                partyList.add(ngoInsightReport.getORGANIZATION().get(orgKey));
                insightsMap.put(insightsKey,partyList);
            }else{
                partyList = insightsMap.get(insightsKey);
                partyList.add(ngoInsightReport.getORGANIZATION().get(orgKey));
            }
        }
        return insightsMap;
    }

    /**
     * To retrieve insights key based on the prediction value received for the NGO party in the insights API response
     * @param prediction prediction value
     * @return insightsKey
     */
    public String retrieveInsightKey(String prediction){
        switch (prediction){
            case NGOConstants.NGO_LOW_PREDICTION_VAL:
            case NGOConstants.NGO_MEDIUM_PREDICTION_VAL:
                return NGOConstants.NGO_PARTIES_WITH_DEFICIT;
            case NGOConstants.NGO_HIGH_PREDICTION_VAL:
            case NGOConstants.NGO_VERY_HIGH_PREDICTION_VAL:
                return NGOConstants.NGO_PARTIES_WITH_SURPLUS;
            default:
                return null;
        }
    }
}
