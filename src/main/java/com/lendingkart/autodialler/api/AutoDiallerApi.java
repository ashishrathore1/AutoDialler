package com.lendingkart.autodialler.api;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.lendingkart.autodialler.beans.PropertiesBean;



@RestController
public class AutoDiallerApi {
	
	@Autowired
	PropertiesBean properties;
	
	
	private static final Logger logger = LoggerFactory.getLogger(AutoDiallerApi.class);
	private static final int MAX_RETRIES = 3;
	private ResponseEntity<String> result = new ResponseEntity<String>("{ ERROR : TimeOutOccured }", HttpStatus.BAD_REQUEST);
	private ResponseEntity<String> loginResult;
	/*for storing key in cache*/
	private String keyId = "0d2cbc60";
	
	@Cacheable(value="clsAuthToken", key="#id")
	public String getToken(String id){	
		String uri = properties.getLoginURI();
		String token = null;
		try{
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "multipart/form-data");
			MultiValueMap<String,String> parameters = new LinkedMultiValueMap<String,String>();
			parameters.add("grant_type", properties.getLoginGrantType());
			parameters.add("client_id", properties.getLoginClientId());
			parameters.add("client_secret", properties.getLoginClientSecret());
			parameters.add("username", properties.getLoginUsername());
			parameters.add("password", properties.getLoginPassword());
			HttpEntity<MultiValueMap<String,String>> entity =
					new HttpEntity<MultiValueMap<String, String>>(parameters, headers);
			loginResult = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			JSONObject response = new JSONObject(loginResult.getBody());
			token = response.getString("access_token");
		}catch(HttpClientErrorException ex){
			System.out.println("CLS LOGIN ERROR STATUS:"+ex.getStatusCode());
			logger.error("CLS LOGIN ERROR STATUS:"+ex.getStatusCode());
		}
		return token;
	}
	
	@CachePut(value="clsAuthToken", key="#id")
	public String updateToken(String id){
		String uri = properties.getLoginURI();
		String token = null;
		try{
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "multipart/form-data");
			MultiValueMap<String,String> parameters = new LinkedMultiValueMap<String,String>();
			parameters.add("grant_type", properties.getLoginGrantType());
			parameters.add("client_id", properties.getLoginClientId());
			parameters.add("client_secret", properties.getLoginClientSecret());
			parameters.add("username", properties.getLoginUsername());
			parameters.add("password", properties.getLoginPassword());
			HttpEntity<MultiValueMap<String,String>> entity =
					new HttpEntity<MultiValueMap<String, String>>(parameters, headers);
			loginResult = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			JSONObject response = new JSONObject(loginResult.getBody());
			token = response.getString("access_token");
		}catch(HttpClientErrorException ex){
			System.out.println("CLS LOGIN ERROR STATUS:"+ex.getStatusCode());
			logger.error("CLS LOGIN ERROR STATUS:"+ex.getStatusCode());
		}
		return token;
	}
	
	
	@RequestMapping(value = "/autodialler/getLeadInfo/{mobileNumber}", method = RequestMethod.POST)
	public String getLeadInfo(@PathVariable("mobileNumber") long mobileNumber,HttpServletRequest request){
	
		
		if(request.getHeader("lendingkart_token").equals(properties.getLendingkartToken())){
			
			
			logger.info("getting token from cache");
			String auth_token = "Bearer "+ getToken(keyId);
			logger.info("token received from cache:" +  auth_token);
			String uri = properties.getClsURI();
			for(int i=0;i<MAX_RETRIES;i++){
				try{
					String finalUri = uri + "?MobileNo=" + mobileNumber;
					RestTemplate restTemplate = new RestTemplate();
					HttpHeaders headers = new HttpHeaders();
					headers.add("Content-Type", "application/json");
					headers.add("Authorization", auth_token);
					HttpEntity<String> httpEntity = new HttpEntity<String>("parameters",headers);
					result = restTemplate.exchange(finalUri, HttpMethod.GET, httpEntity, String.class);
				}catch(HttpClientErrorException ex){
					auth_token = "Bearer ";
					logger.info("getting token hitting api");
					String tokenNo = updateToken(keyId);
					logger.info("token received from hitting api:" +  tokenNo);
					if(tokenNo!=null){
						auth_token += tokenNo;
					}
					continue;
				}
				
			}
		}else{
			
			return "{ Error : Unauthenticated lendingkart token }";
		}
		
		
		return result.getBody();
	}
	
	
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String hello(){
		
		
		return "AUTODIALLER APPLICATION UP!!! SUCCESSFULLY";
	}
	
	

}


