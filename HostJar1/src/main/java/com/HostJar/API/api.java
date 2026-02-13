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
import com.fasterxml.jackson.databind.ObjectMapper;

import DTO.AccountListResponse;
import DTO.CreditCardResponse;
import DTO.CustomerCardDetails;
import DTO.GenerateOTPResponse;
import DTO.IdentifyCustomerResponse;
import DTO.NrmnValidationResponse;
import DTO.SetPreferredLanguageResponse;
import DTO.TpinResponse;
import DTO.ValidateOTPResponse;

public class api {

	private static final ObjectMapper mapper = new ObjectMapper();
	// private static final Logger logger =LoggerFactory.getLogger(api.class);

	// Create ONE secure HttpClient using a single .p12 (contains 2 certs)
	private static volatile String accessToken = null;
	private static volatile long tokenExpiryTime = 0;

	private static final Object tokenLock = new Object();

	// ✅ ALWAYS use singleton
	private static final Load config = Load.getInstance();

	// =====================================================
	// ✅ LOAD CONFIG ONCE (VERY IMPORTANT)
	// =====================================================

	private static String requireProperty(String key) {

		String value = config.getProperty(key);

		if (value == null || value.trim().isEmpty()) {
			throw new RuntimeException("Missing property in Config.properties → " + key);
		}

		return value.trim();
	}

	static {

		try {

			String configPath = "D:/Banking_Project/Config/Config.properties";

			Load.init(configPath);

			System.out.println("✅ Config initialized in API");

		} catch (Exception e) {

			throw new RuntimeException("Failed to load config at startup", e);
		}
	}

	private static void invalidateToken() {

		accessToken = null;
		tokenExpiryTime = 0;
	}

	// =====================================================
	// ✅ BUFFER TIME
	// =====================================================

	private static long getBufferTime() {

		return config.getLong("OAUTH_BUFFER_SECONDS", 300) * 1000;
	}

	// =====================================================
	// ✅ CREATE SECURE HTTP CLIENT
	// =====================================================

	public static void createSecureHttpClient() throws Exception {

		if (Load.getClient() != null)
			return;

		synchronized (api.class) {

			if (Load.getClient() != null)
				return;

			System.out.println("🔐 Creating Secure HttpClient...");

			String p12Path = requireProperty("p12Path");
			String p12Password = requireProperty("p12Password");

			KeyStore keyStore = KeyStore.getInstance("PKCS12");

			try (FileInputStream fis = new FileInputStream(p12Path)) {
				keyStore.load(fis, p12Password.toCharArray());
			}

			SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, p12Password.toCharArray())
					.loadTrustMaterial(keyStore, null).build();

			CloseableHttpClient client = HttpClients.custom().setSSLContext(sslContext).build();

			Load.setClient(client);

			System.out.println("✅ Secure HttpClient Created");
		}
	}

	// =====================================================
	// ✅ GET OAUTH TOKEN (BANKING SAFE)
	// =====================================================

	public static String getOAuthToken() throws Exception {

		long now = System.currentTimeMillis();
		long buffer = config.getLong("OAUTH_BUFFER_SECONDS", 300) * 1000;

		if (accessToken != null && now < (tokenExpiryTime - buffer)) {
			return accessToken;
		}

		synchronized (tokenLock) {

			now = System.currentTimeMillis();

			if (accessToken != null && now < (tokenExpiryTime - buffer)) {
				return accessToken;
			}

			createSecureHttpClient();

			CloseableHttpClient client = Load.getClient();

			System.out.println("🔐 Generating NEW OAuth token...");

			HttpPost post = new HttpPost(requireProperty("OAUTH_TOKEN_URL"));

			List<NameValuePair> params = new ArrayList<>();

			params.add(new BasicNameValuePair("grant_type", requireProperty("OAUTH_GRANT_TYPE")));

			params.add(new BasicNameValuePair("client_id", requireProperty("OAUTH_CLIENT_ID")));

			params.add(new BasicNameValuePair("client_secret", requireProperty("OAUTH_CLIENT_SECRET")));

			post.setEntity(new UrlEncodedFormEntity(params));
			post.setHeader("Accept", "application/json");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");

			try (CloseableHttpResponse response = client.execute(post)) {

				String body = EntityUtils.toString(response.getEntity());

				if (response.getStatusLine().getStatusCode() != 200) {
					throw new RuntimeException("OAuth failed → " + body);
				}

				JSONObject json = new JSONObject(body);

				accessToken = json.getString("access_token");

				long expiresIn = json.optLong("expires_in", 3600);

				tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);

				System.out.println("✅ Token Generated. Valid for " + expiresIn + " sec");

				return accessToken;
			}
		}
	}

	public static IdentifyCustomerResponse RMNIdentificationAPI(String ucid, String mobilenumber, String requesttime)
			throws Exception {

		String apiUrl = requireProperty("RMN_VALIDATE");

		// Ensure HttpClient is initialized
		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			System.out.println("[API] HttpClient is NULL. Creating new client...");
			createSecureHttpClient();
			client = Load.getClient();
		}

		// Build request JSON
		JSONObject json = new JSONObject();
		json.put("ucid", ucid);
		json.put("mobileNumber", mobilenumber);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		String body;

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			body = EntityUtils.toString(response.getEntity());

			// Token expired? retry once
			if (statusCode == 401 || statusCode == 403) {
				System.out.println("[OAuth] Token rejected. Refreshing...");
				invalidateToken();
				token = getOAuthToken();

				// Ensure client is valid
				client = Load.getClient();
				if (client == null) {
					createSecureHttpClient();
					client = Load.getClient();
				}

				HttpPost retryPost = new HttpPost(apiUrl);
				retryPost.setHeader("Authorization", "Bearer " + token);
				retryPost.setHeader("Accept", "application/json");
				retryPost.setHeader("Content-Type", "application/json");
				retryPost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

				try (CloseableHttpResponse retryResponse = client.execute(retryPost)) {
					body = EntityUtils.toString(retryResponse.getEntity());
				}
			}
		}

		// ------------------- Convert JSON string to DTO -------------------
		IdentifyCustomerResponse responseDto = mapper.readValue(body, IdentifyCustomerResponse.class);

		System.out.println("Response Code: " + responseDto.getResponseCode());
		System.out.println("Customer Mobile: " + responseDto.getData().getMobileNumber());
		System.out.println("Has TPIN: " + responseDto.getData().getHasTPIN());

		return responseDto;
	}

	// ------------------------ SetPreferredLanguageAPI ------------------------
	public static SetPreferredLanguageResponse SetPreferredLanguageAPI(String ucid, String relationShipID,
			String preferredLanguage, String requesttime) throws Exception {

		String apiUrl = requireProperty("SET_PREFERRED_LANG");

		// Ensure HttpClient is initialized
		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			System.out.println("[API] HttpClient is NULL. Creating new client...");
			createSecureHttpClient();
			client = Load.getClient();
		}

		// Build request JSON
		JSONObject json = new JSONObject();
		json.put("RelationshipID", relationShipID);
		json.put("preferredLanguage", preferredLanguage);
		json.put("ucid", ucid);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		String body;

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			body = EntityUtils.toString(response.getEntity());

			System.out.println("SetPreferredLanguageAPI | Status: " + statusCode);
			System.out.println("SetPreferredLanguageAPI | Response: " + body);

			// Retry once if token expired
			if (statusCode == 401 || statusCode == 403) {
				System.out.println("[OAuth] Token rejected. Refreshing...");
				invalidateToken();
				token = getOAuthToken();

				client = Load.getClient();
				if (client == null) {
					createSecureHttpClient();
					client = Load.getClient();
				}

				HttpPost retryPost = new HttpPost(apiUrl);
				retryPost.setHeader("Authorization", "Bearer " + token);
				retryPost.setHeader("Accept", "application/json");
				retryPost.setHeader("Content-Type", "application/json");
				retryPost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

				try (CloseableHttpResponse retryResponse = client.execute(retryPost)) {
					body = EntityUtils.toString(retryResponse.getEntity());
					System.out.println("SetPreferredLanguageAPI | Retry Response: " + body);
				}
			}
		}

		SetPreferredLanguageResponse responseDto = mapper.readValue(body, SetPreferredLanguageResponse.class);
		System.out.println("Response Code: " + responseDto.getResponseCode());
		System.out.println("Message: " + responseDto.getMessage());

		return responseDto;
	}

	// ------------------------ AccountList ------------------------
	public static AccountListResponse AccountList(String ucid, String relationshipID, String requestTime)
			throws Exception {

		String apiUrl = requireProperty("ACC_LIST");
		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			createSecureHttpClient();
			client = Load.getClient();
		}

		JSONObject json = new JSONObject();
		json.put("ucid", ucid);
		json.put("RelationshipID", relationshipID);
		json.put("requestTime", requestTime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			String body = EntityUtils.toString(response.getEntity());

			if (statusCode == 401 || statusCode == 403) {
				invalidateToken();
				token = getOAuthToken();
				post.setHeader("Authorization", "Bearer " + token);
				try (CloseableHttpResponse retryResponse = client.execute(post)) {
					body = EntityUtils.toString(retryResponse.getEntity());
				}
			}

			// Use ObjectMapper to directly map JSON to POJO
			AccountListResponse pojo = mapper.readValue(body, AccountListResponse.class);
			return pojo;
		}
	}

	// ------------------------ GenerateOTP ------------------------
	public static GenerateOTPResponse GenerateOTP(String ucid, String RelationShipID, String mobilenumber,
			String Channel, String requesttime) throws Exception {

		String apiUrl = requireProperty("OTP_GENERATE");
		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			createSecureHttpClient();
			client = Load.getClient();
		}

		String requestId = UUID.randomUUID().toString();

		JSONObject json = new JSONObject();
		json.put("RelationshipID", RelationShipID);
		json.put("mobileNumber", mobilenumber);
		json.put("channel", Channel);
		json.put("ucid", ucid);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		String body;

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			body = EntityUtils.toString(response.getEntity());

			System.out.println("GenerateOTP | RequestId: " + requestId);
			System.out.println("API Status: " + statusCode);
			System.out.println("API Response: " + body);

			if (statusCode == 401 || statusCode == 403) {
				System.out.println("[OAuth] Token rejected in GenerateOTP. Refreshing...");
				invalidateToken();
				token = getOAuthToken();
				post.setHeader("Authorization", "Bearer " + token);
				try (CloseableHttpResponse retryResponse = client.execute(post)) {
					body = EntityUtils.toString(retryResponse.getEntity());
					System.out.println("GenerateOTP | Retry Response: " + body);
				}
			}
		}

		GenerateOTPResponse responseDto = mapper.readValue(body, GenerateOTPResponse.class);

		return responseDto;
	}

	// ------------------------ ValidateOTP ------------------------
	public static ValidateOTPResponse ValidateOTP(String ucid, String RelationShipID, String otpReference,
			String isEncrypted, String mobilenumber, String otp, String requesttime) throws Exception {

		String apiUrl = requireProperty("OTP_VALIDATE");

		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			createSecureHttpClient();
			client = Load.getClient();
		}

		String requestId = UUID.randomUUID().toString();

		JSONObject json = new JSONObject();
		json.put("RelationshipID", RelationShipID);
		json.put("otpReference", otpReference);
		json.put("otp", otp);
		json.put("isEncrypted", isEncrypted);
		json.put("mobileNumber", mobilenumber);
		json.put("ucid", ucid);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		String body;

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			body = EntityUtils.toString(response.getEntity());

			System.out.println("ValidateOTP | RequestId: " + requestId);
			System.out.println("API Status: " + statusCode);
			System.out.println("API Response: " + body);

			if (statusCode == 401 || statusCode == 403) {
				System.out.println("[OAuth] Token rejected in ValidateOTP. Refreshing...");
				invalidateToken();
				token = getOAuthToken();
				post.setHeader("Authorization", "Bearer " + token);
				try (CloseableHttpResponse retryResponse = client.execute(post)) {
					body = EntityUtils.toString(retryResponse.getEntity());
					System.out.println("ValidateOTP | Retry Response: " + body);
				}
			}
		}

		// Convert JSON string to POJO
		ValidateOTPResponse responseDto = mapper.readValue(body, ValidateOTPResponse.class);

		return responseDto;
	}

	public static CreditCardResponse CreditCardList(String ucid, String RelationShipID, String requesttime)
			throws Exception {

		String apiUrl = requireProperty("CREDIT_CARD_LIST");

		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			createSecureHttpClient();
			client = Load.getClient();
		}

		String requestId = UUID.randomUUID().toString();

		JSONObject json = new JSONObject();
		json.put("relationshipId", RelationShipID);
		json.put("ucid", ucid);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		String body;

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			body = EntityUtils.toString(response.getEntity());

			System.out.println("CreditCardList | RequestId: " + requestId);
			System.out.println("API Status: " + statusCode);
			System.out.println("API Response: " + body);

			// Retry if token expired
			if (statusCode == 401 || statusCode == 403) {
				System.out.println("[OAuth] Token rejected in CreditCardList. Refreshing...");
				invalidateToken();
				token = getOAuthToken();
				post.setHeader("Authorization", "Bearer " + token);
				try (CloseableHttpResponse retryResponse = client.execute(post)) {
					body = EntityUtils.toString(retryResponse.getEntity());
					System.out.println("CreditCardList | Retry Response: " + body);
				}
			}
		}

		CreditCardResponse responseDto = mapper.readValue(body, CreditCardResponse.class);

		return responseDto;
	}

	public static TpinResponse ValidateTPIN(String isEncrypted, String TPIN, String ucid, String RelationShipID,
			String requesttime) throws Exception {

		String apiUrl = requireProperty("TPIN_AUTHENTICATION");

		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			createSecureHttpClient();
			client = Load.getClient();
		}

		String requestId = UUID.randomUUID().toString();

		JSONObject json = new JSONObject();
		json.put("relationshipID", RelationShipID);
		json.put("tpin", TPIN);
		json.put("ucid", ucid);
		json.put("isEncrypted", isEncrypted);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		try (CloseableHttpResponse response = client.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
			String body = EntityUtils.toString(response.getEntity());

			System.out.println("ValidateTPIN | RequestId: " + requestId);
			System.out.println("API Status: " + statusCode);
			System.out.println("API Response: " + body);

// Retry if token expired
			if (statusCode == 401 || statusCode == 403) {
				System.out.println("[OAuth] Token rejected in ValidateTPIN. Refreshing...");
				invalidateToken();
				token = getOAuthToken();
				post.setHeader("Authorization", "Bearer " + token);
				try (CloseableHttpResponse retryResponse = client.execute(post)) {
					body = EntityUtils.toString(retryResponse.getEntity());
					System.out.println("ValidateTPIN | Retry Response: " + body);
				}
			}

// Map JSON response to POJO
			TpinResponse pojo = mapper.readValue(body, TpinResponse.class);
			return pojo;
		}
	}

	// ------------------------ NRMNidentificationAPI ------------------------
	public static NrmnValidationResponse NRMNidentificationAPI(String ucid, String AccNum, String AccType, String requesttime)
	        throws Exception {

	    String apiUrl = requireProperty("NRMN_IDENTIFICATION");

	    CloseableHttpClient client = Load.getClient();
	    if (client == null) {
	        createSecureHttpClient();
	        client = Load.getClient();
	    }

	    String requestId = UUID.randomUUID().toString();

	    JSONObject json = new JSONObject();
	    json.put("accNum", AccNum);
	    json.put("accType", AccType);
	    json.put("ucid", ucid);
	    json.put("requestTime", requesttime);

	    String token = getOAuthToken();

	    HttpPost post = new HttpPost(apiUrl);
	    post.setHeader("Authorization", "Bearer " + token);
	    post.setHeader("Accept", "application/json");
	    post.setHeader("Content-Type", "application/json");
	    post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

	    try (CloseableHttpResponse response = client.execute(post)) {
	        int statusCode = response.getStatusLine().getStatusCode();
	        String body = EntityUtils.toString(response.getEntity());

	        System.out.println("NRMNidentificationAPI | RequestId: " + requestId);
	        System.out.println("API Status: " + statusCode);
	        System.out.println("API Response: " + body);

	        // Retry if token expired
	        if (statusCode == 401 || statusCode == 403) {
	            System.out.println("[OAuth] Token rejected in NRMNidentificationAPI. Refreshing...");
	            invalidateToken();
	            token = getOAuthToken();
	            post.setHeader("Authorization", "Bearer " + token);
	            try (CloseableHttpResponse retryResponse = client.execute(post)) {
	                body = EntityUtils.toString(retryResponse.getEntity());
	                System.out.println("NRMNidentificationAPI | Retry Response: " + body);
	            }
	        }

	        // Map JSON response to POJO
	        NrmnValidationResponse pojo = mapper.readValue(body, NrmnValidationResponse.class);
	        return pojo;
	    }
	}

	// ------------------------ GenerateandChangeTPIN ------------------------
	public static String GenerateandChangeTPIN(String RelationshipID, String action, String tpin,
			String isEncrypted, String ucid, String requesttime) throws Exception {

		String apiUrl = requireProperty("TPIN");

		CloseableHttpClient client = Load.getClient();
		if (client == null) {
			createSecureHttpClient();
			client = Load.getClient();
		}
		
		String requestId = UUID.randomUUID().toString();

		JSONObject json = new JSONObject();
		json.put("relationshipID", RelationshipID);
		json.put("action", action);
		json.put("tpin", tpin);
		json.put("isEncrypted", isEncrypted);
		json.put("ucid", ucid);
		json.put("requestTime", requesttime);

		String token = getOAuthToken();

		HttpPost post = new HttpPost(apiUrl);
		post.setHeader("Authorization", "Bearer " + token);
		post.setHeader("Accept", "application/json");
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

		try (CloseableHttpResponse response = client.execute(post)) {

			int statusCode = response.getStatusLine().getStatusCode();
			String body = EntityUtils.toString(response.getEntity());

			System.out.println("GenerateandChangeTPIN | RequestId: " + requestId);
			System.out.println("API Status: " + statusCode);
			System.out.println("API Response: " + body);

			// retry logic
			if (statusCode == 401 || statusCode == 403) {

				System.out.println("[OAuth] Token rejected. Refreshing...");
				invalidateToken();

				token = getOAuthToken();
				post.setHeader("Authorization", "Bearer " + token);

				try (CloseableHttpResponse retryResponse = client.execute(post)) {

					String retryBody = EntityUtils.toString(retryResponse.getEntity());
					//return mapper.readValue(retryBody, TpinResponse.class);
				}
			}

			return body;
			// ⭐ Convert JSON → POJO
//			return mapper.readValue(body, TpinResponse.class);
		}
	}

	public static void main(String[] args) {

		try {
			// Step 1: Create a secure HttpClient
			createSecureHttpClient();

			// Step 2: Get OAuth token
			String token = getOAuthToken();

//			// Step 3: Call APIs using the token
//			IdentifyCustomerResponse rmnResponse = RMNIdentificationAPI("00012345678", "9998887778",
//					"2026-01-14T10:35:00Z");
//			System.out.println("RMN Response: " + rmnResponse);
//
		//SetPreferredLanguageResponse setPreferredLanguageResponse = SetPreferredLanguageAPI("00012345678",
			//	"REL000001", "EN", "2026-01-14T10:35:00Z");
		//System.out.println("Prefered Response: " + setPreferredLanguageResponse);
//
//			AccountListResponse accListResponse = AccountList("00012345678", "REL000001", "2026-01-14T10:35:00Z");
//			System.out.println("Account List Response: " + accListResponse);
//
//			List<AccountListResponse.Account> accounts = accListResponse.getData().getAccounts();
//			for (AccountListResponse.Account acc : accounts) {
//				System.out.println("Account Number: " + acc.getAccountNumber());
//			}
//
//			GenerateOTPResponse otpResponse = GenerateOTP("00012345678", "REL000001", "9998887777", "SMS",
//					"2026-02-11T10:00:00Z");
//			System.out.println("OTP Reference: " + otpResponse.getOtpReference());
//			System.out.println("Message: " + otpResponse.getMessage());
//			System.out.println("Expiry: " + otpResponse.getExpiry());
//
//			// Example: Validate OTP
//			ValidateOTPResponse otpResp = ValidateOTP("00012345678", "REL000001", "OTPREF001", "N", "9998887777",
//					"123456", "2026-02-11T10:00:00Z");
//
//			System.out.println("Response Code: " + otpResp.getResponseCode());
//			System.out.println("Message: " + otpResp.getMessage());
//
//			
			TpinResponse tpinResp = tpinResp = ValidateTPIN("Y", "2345", "00012345678", "REL000002", "2026-02-11T10:00:00Z");
            System.out.println("TPIN Response Code: " + tpinResp.getResponseCode());
       		System.out.println("TPIN Message: " + tpinResp.getMessage());
//
//			
//			// Example: Credit Card List
//			CreditCardResponse ccResp = CreditCardList("00012345678", "REL000001", "2026-02-11T10:00:00Z");
//
//			System.out.println("Response Code: " + ccResp.getResponseCode());
//			System.out.println("Message: " + ccResp.getMessage());
//
//			for (CustomerCardDetails card : ccResp.getCreditCards()) {
//				System.out.println("Card Number: " + card.getCreditCardNumber());
//				System.out.println("Card Type: " + card.getCreditCardType());
//			}
//
//			// Example: Validate TPIN
//			TpinResponse tpinResponse = ValidateTPIN("N", "1234", "00012345678", "REL000001", "2026-01-14T10:35:00Z");
//			System.out.println("TPIN Response Code: " + tpinResp.getResponseCode());
//			System.out.println("TPIN Message: " + tpinResp.getMessage());
//
//			
//			NrmnValidationResponse response = NRMNidentificationAPI("00012345678", "12345678901", "ACCNO", "2026-02-11T10:00:00Z");
//
//			System.out.println("Response Code: " + response.getResponseCode());
//			System.out.println("Message: " + response.getMessage());
//			System.out.println("Mobile Number: " + response.getData().getMobileNumber());
//
//			
//			for (NrmnValidationResponse.Product product : response.getData().getProducts()) {
//			    System.out.println("Product Number: " + product.getNumber());
//			    System.out.println("Product Type: " + product.getType());
//			    System.out.println("Account Type: " + product.getAccountType());
//			}
			String genChangeTpinResponse = GenerateandChangeTPIN("REL000002", "GENERATE", "1234", "N",
					"12345678901", "2026-01-14T10:35:00Z");
			System.out.println("Generate/Change TPIN Response: " + genChangeTpinResponse);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}