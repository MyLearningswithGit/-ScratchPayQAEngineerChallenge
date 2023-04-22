package com.scratchpay.ApplicationTests;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.scratchpay.APITests.APITests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;


// Below test cover scenarios under qa-challenge-application.zip. Please run the project locally in order to make sure
// APIs are working before we test them.
// I have tried covering two scenarios as per the APIs
// 1. API - http://localhost:3000/api/v1/settlementDate - test is currently failing as values are null
// 2. API - http://localhost:3000/api/v1/isBusinessDay

public class ApplicationTests {
	
	Properties properties = new Properties();

	@BeforeClass
	public void getData() throws IOException {
		
		InputStream inputStream = getClass().getResourceAsStream("/com/scratchpay/configs/config.properties");
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		properties.load(inputStream);
	}
	
	@Test(description = "Validate Settlement Date API", priority = 0)
	public void validateSettlementDate() {
		RestAssured.baseURI = properties.getProperty("applicationURL");
		RestAssured.registerParser("text/plain", Parser.JSON);

		//Fetching response for SettlementDate API
		String response = given().log().all()
				.when().get("api/v1/settlementDate").then().assertThat()
				.statusCode(SC_OK).body("ok", equalTo(true)).and().contentType(ContentType.JSON).extract().response()
				.asString();

		System.out.println(response);

		// Fetching values from the response 
		JsonPath jsonPath = new JsonPath(response);		
		String businessDate = jsonPath.getString("results.businessDate");
		String holidayDays = jsonPath.getString("results.holidayDays");
		String totalDays = jsonPath.getString("results.totalDays");
		String weekendDays = jsonPath.getString("results.weekendDays");
		Assert.assertNotNull(businessDate);
		Assert.assertNotNull(holidayDays);
	}
	
	@Test(description = "Validate Business Day API", priority = 1)
	public void validateifEnteredDateisABusinessDay() {
		RestAssured.baseURI = properties.getProperty("applicationURL");
		RestAssured.registerParser("text/plain", Parser.JSON);
		
		//Fetching response for Business Day API
		String response = given().log().all()
				.when().get("api/v1/isBusinessDay").then().assertThat()
				.statusCode(SC_OK).body("ok", equalTo(false)).and().contentType(ContentType.JSON).extract().response()
				.asString();

		System.out.println(response);

		// Validating the response parameters 
		JsonPath jsonPath = new JsonPath(response);		
		String errorMessage = jsonPath.getString("errorMessage");
		Assert.assertEquals(errorMessage, "A valid date is required");
	}
}
