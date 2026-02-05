package com.HostJar.API;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.HostJar.LoadValues.Load;

public class api {


    // Create ONE secure HttpClient using a single .p12 (contains 2 certs)

    public static void createSecureHttpClient() throws Exception {
    	String  p12Path = Load.CONFIG.getProperty("p12Path");
    	String p12Password = Load.CONFIG.getProperty("p12Password");
      	    	
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            keyStore.load(fis, p12Password.toCharArray());
        }

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, p12Password.toCharArray()) // client certs (OAuth + API)
                .loadTrustMaterial(keyStore, null)                    // server certs
                .build();

        CloseableHttpClient Client =  HttpClients.custom()
                .setSSLContext(sslContext)
                .build();	
        
        Load.setClient(Client);
    }

    // OAuth Token Generation (client_credentials)

    public static String getOAuthToken() throws Exception {
    	
    	
    	String  oauthUrl = Load.CONFIG.getProperty("OAUTH_TOKEN_URL");
    	String  clientId = Load.CONFIG.getProperty("OAUTH_CLIENT_ID");
    	String  clientSecret = Load.CONFIG.getProperty("OAUTH_CLIENT_SECRET");
    	String  grantType = Load.CONFIG.getProperty("OAUTH_GRANT_TYPE");
    	
    	CloseableHttpClient client = Load.getClient();

        HttpPost post = new HttpPost(oauthUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", grantType));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));

        post.setEntity(new UrlEncodedFormEntity(params));
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            

            System.out.println("[OAuth] Status Code : " + statusCode);
            System.out.println("[OAuth] Response    : " + body);

            if (statusCode == 200) {
                JSONObject json = new JSONObject(body);
                return json.getString("access_token");
            }
            return null;
        }
    }

    // API Call using OAuth token (mTLS + Bearer Token)
    
    public static String RMNIdentificationAPI(String token, String ucid,String mobilenumber, String requesttime) throws Exception {

    	String  apiUrl = Load.CONFIG.getProperty("RMN_VALIDATE");
    	
    	CloseableHttpClient client = Load.getClient();
    	
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");

        JSONObject json = new JSONObject();
        json.put("ucid", ucid);
        json.put("mobileNumber", mobilenumber);
        json.put("requestTime", requesttime);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    
    public static String SetPreferredLanguageAPI(String token, String ucid,String RelationShipID,String PreferredLanguage, String requesttime) throws Exception {

    	
String  apiUrl = Load.CONFIG.getProperty("SET_PREFERRED_LANG");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");

        JSONObject json = new JSONObject();
        json.put("RelationshipID",RelationShipID );
        json.put("preferredLanguage", PreferredLanguage);
        json.put("ucid", ucid);
        json.put("requestTime", requesttime);
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String AccountList(String token, String ucid,String RelationShipID, String requesttime) throws Exception {

    	
String  apiUrl = Load.CONFIG.getProperty("ACC_LIST");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");

        JSONObject json = new JSONObject();
        json.put("ucid", ucid);
        json.put("RelationshipID", RelationShipID);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String GenerateOTP(String token, String ucid,String RelationShipID,String mobilenumber, String Channel,String requesttime) throws Exception {

    	
String  apiUrl = Load.CONFIG.getProperty("OTP_GENERATE");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");

        JSONObject json = new JSONObject();
        json.put("RelationshipID", RelationShipID);
        json.put("mobileNumber", mobilenumber);
        json.put("channel", Channel);
        json.put("ucid", ucid);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String ValidateOTP(String token, String ucid,String RelationShipID,String otpReference,String isEncrypted,String mobilenumber, String otp,String requesttime) throws Exception {

    	
String  apiUrl = Load.CONFIG.getProperty("OTP_VALIDATE");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);
        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");


        JSONObject json = new JSONObject();
        json.put("RelationshipID", RelationShipID);
        json.put("otpReference", otpReference);
        json.put("otp", otp);
        json.put("isEncrypted", isEncrypted);
        json.put("mobileNumber", mobilenumber);
        json.put("ucid", ucid);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String CreditCardList(String token, String ucid,String RelationShipID,String requesttime) throws Exception {

    	
String  apiUrl = Load.CONFIG.getProperty("CREDIT_CARD_LIST");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");


        JSONObject json = new JSONObject();
        json.put("relationshipId", RelationShipID);
        json.put("ucid", ucid);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String ValidateTPIN(String token,String isEncrypted,String TPIN,String ucid,String RelationShipID,String requesttime) throws Exception {

    	
String  apiUrl = Load.CONFIG.getProperty("TPIN_AUTHENTICATION");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");


        JSONObject json = new JSONObject();
        json.put("relationshipId", RelationShipID);
        json.put("tpin", TPIN);
        json.put("ucid", ucid);
        json.put("isEncrypted", isEncrypted);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String NRMNidentificationAPI(String token,String ucid,String AccNum,String AccType,String requesttime) throws Exception {

    	
    String  apiUrl = Load.CONFIG.getProperty("NRMN_IDENTIFICATION");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();
        
        System.out.println(apiUrl);
        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");


        JSONObject json = new JSONObject();
        json.put("accNum", AccNum);
        json.put("accType", AccType);
        json.put("ucid", ucid);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);

        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    public static String GenerateandChangeTPIN(String token,String RelationshipID,String action,String tpin,String isEncrypted,String ucid ,String requesttime) throws Exception {

String  apiUrl = Load.CONFIG.getProperty("TPIN");
    	
    	CloseableHttpClient client = Load.getClient();
        String requestId = UUID.randomUUID().toString();

        HttpPost post = new HttpPost(apiUrl);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");


        JSONObject json = new JSONObject();
        json.put("RelationshipID", RelationshipID);
        json.put("action", action);
        json.put("tpin", tpin);
        json.put("isEncrypted", isEncrypted);
        json.put("ucid", ucid);
        json.put("requestTime", requesttime);
       
        System.out.println("json : " + json);
        post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            System.out.println("Request ID     : " + requestId);
            System.out.println("API Status     : " + statusCode);
            System.out.println("API Response   : " + body);

            return body;
        }
    }
    

    public static void main(String[] args) {

        try {
            String p12Path = "D:/Banking_Project/JKS/client-truststore.p12";
            String p12Password = "changeit";

            String oauthUrl = "https://localhost:8091/oauth/token";


            String clientId = "my-client-id";
            String clientSecret = "my-client-secret";

            // 1️ Create ONE mTLS client
             createSecureHttpClient();

             
            String token = getOAuthToken();
                 
           // NRMNidentificationAPI(token, "00012345678", "12345678901", "ACCNO", "2026-01-14T10:35:00Z");
            

            //System.out.println("Access Token : " + token);

            // 3️ Call API
        RMNIdentificationAPI( token,"00012345678","9998887778","2026-01-14T10:35:00Z");
       
          SetPreferredLanguageAPI( token, "00012345678", "REL000001","EN", "2026-01-14T10:35:00Z");
//            
          AccountList( token, "00012345678", "REL000001", "2026-01-14T10:35:00Z");
//            
        GenerateOTP(token,"00012345678","REL000001","9998887777","SMS","2026-01-14T10:35:00Z");
//            
            ValidateOTP( token,"00012345678","REL000001","d248a8c2-","N","9998887777","186193","2026-01-14T10:35:00Z");
//            
           CreditCardList(token, "00012345678", "REL000001", "2026-01-14T10:35:00Z");
           System.out.println("TPIN");
            ValidateTPIN( token,"N","2123","00012345678","REL000001","2026-01-14T10:35:00Z");
//            System.out.println("nrmn");
          NRMNidentificationAPI(token, "00012345678", "12345678901", "ACCNO", "2026-01-14T10:35:00Z");
          
//GenerateandChangeTPIN(client, "https://localhost:8090/Customer/generate", token, "REL000002", "GENERATE", "2123","N","000123456789", "2026-01-14T10:35:00Z");
           
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}