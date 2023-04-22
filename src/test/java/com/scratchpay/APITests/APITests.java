package com.scratchpay.APITests;

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
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

// Below test cover "API Tests" Challenge with multiple scenarios 
// Eight scenarios have been covered
// a. Scenario 1 - is to validate user login where we are able to login and validate values 
// for loggedIn and auth value to make sure they are true. Also I have added assertion for token value if its null or not.
// b. Scenario 2 - This unit test covers Scenario 1 under "API tests" challenge where we prevent logged in user from 
// accessing email list 
// c. Scenario 3 - it covers test scenario 2 under "API Tests" challenge where we search for clinics with the word "veterinary"
// along with condition check if user is logged in or not 
// d. Scenario 4 - is to check if permissions tab is empty or not once user is logged in and print the list
// e. Scenario 5 - User is not able to login without email Id and password 
// f. Scenario 6 - User is not able to login with invalid email Id and password   
// g. Scenario 7 - User is not able to search without access token 
// h. Scenario 8 - Validate search using different terms apart from "veterinary" such Dermatology, dental, vision, lasik

public class APITests {

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

	@Test(description = "Validate User Login using email and password", priority = 0)
	public void validateUserLogin() {
		RestAssured.baseURI = properties.getProperty("LoginURL"); // Fetch Base URL from config.properties file 
		RestAssured.registerParser("text/plain", Parser.JSON);

		// Fetch Response body for API call using GET method in a string format using given inputs 
		String response = given().log().all().queryParam("email", properties.getProperty("email"))
				.queryParam("password", properties.getProperty("password")).when().get("auth").then().assertThat()
				.statusCode(SC_OK).body("ok", equalTo(true)).and().contentType(ContentType.JSON).extract().response()
				.asString();

		System.out.println(response);

		JsonPath jsonPath = new JsonPath(response);
		String token = jsonPath.getString("data.session.token"); // Fetching value using JsonPath 
		Boolean loggedInValue = jsonPath.getBoolean("data.session.loggedIn");
		Boolean authValue = jsonPath.getBoolean("data.auth");
		
		// Validating response using the values fetched
		Assert.assertNotNull(token); // Making sure token value is not empty 
		Assert.assertTrue(loggedInValue);// Making sure Logged in Value is true 
		Assert.assertTrue(authValue);// Making sure Auth Value is true for a logged in user 
	}

	@Test(description = "Validate if a user is logged in for a given email ID and password should be prevented from getting the list of practice id 2", priority = 1)
	public void validateIfLoggedInUserIsPreventedFromAccessingEmailListForaDifferentUserId() {
		RestAssured.baseURI = properties.getProperty("LoginURL"); // Fetching Base URL 
		RestAssured.registerParser("text/plain", Parser.JSON);

		String response = given().log().all().queryParam("email", properties.getProperty("email"))
				.queryParam("password", properties.getProperty("password")).when().get("auth").then().assertThat()
				.statusCode(SC_OK).body("ok", equalTo(true)).and().contentType(ContentType.JSON).extract().response()
				.asString();

		System.out.println(response);

		JsonPath jsonPath = new JsonPath(response);
		String token = jsonPath.getString("data.session.token"); // Fetching Token value
		Assert.assertNotNull(token);

		// Making API call using Bearer token in order to fetch the response if user can fetch email list or not
		String response1 = given().log().all().header("Authorization", "Bearer " + token).when().get("clinics/2/emails")
				.then().assertThat().statusCode(400).body("ok", equalTo(false)).and().contentType(ContentType.JSON)
				.extract().response().asString();

		System.out.println(response1);
		JsonPath jsonPath1 = new JsonPath(response1);
		String message = jsonPath1.getString("data.message");// Fetching message from response body
		String error = jsonPath1.getString("data.error");// Fetching error message from response body 

		// Asserting message and error received with expected values 
		Assert.assertEquals(message, "An error happened");
		Assert.assertEquals(error, "Error: User does not have permissions");
	}

	@Test(description = "Validate using a search key word and return the results based on whether user is logged in or not", priority = 2)
	public void validateForSearchKeyWordBasedOnWheatherUserIsLoggedInOrNot() {
		RestAssured.baseURI = properties.getProperty("LoginURL");
		RestAssured.registerParser("text/plain", Parser.JSON);

		// Making API call to login in order to fetch token value and make sure user is logging in with correct credentials
		String response = given().log().all().queryParam("email", properties.getProperty("email"))
				.queryParam("password", properties.getProperty("password")).when().get("auth").then().assertThat()
				.statusCode(SC_OK).body("ok", equalTo(true)).and().contentType(ContentType.JSON).extract().response()
				.asString();

		System.out.println(response);

		JsonPath jsonPath = new JsonPath(response);
		String token = jsonPath.getString("data.session.token");// Fetching token value 
		Boolean loggedInValue = jsonPath.getBoolean("data.session.loggedIn"); // Fetching Logged in Status of user 
		Assert.assertNotNull(token);

		// If user loggedInvalue is true ten only user can make API call for specific search term else it will throw error
		if (loggedInValue) {
			String response1 = given().log().all().header("Authorization", "Bearer " + token).when()
					.get("clinics?term=veterinary").then().assertThat().statusCode(200).body("ok", equalTo(true)).and()
					.contentType(ContentType.JSON).extract().response().asString();

			System.out.println(response1);
			JsonPath jsonPath1 = new JsonPath(response1);
			int dataSize = jsonPath1.getInt("data.size()");
			if (dataSize == 0) {
				Assert.assertTrue(dataSize > 0);
			} else {
				// Printing values inside Data Array if data is not empty
				System.out.println("Printing the values for Id and DisplayName inside Data:  ");
				for (int i = 0; i < dataSize; i++) {
					System.out.println(jsonPath1.getString("data[" + i + "].id"));
					System.out.println(jsonPath1.getString("data[" + i + "].displayName"));
				}
			}
		} else {
			Assert.assertTrue(loggedInValue);
		}
	}

	@Test(description = "Check if Permissions Tab is not empty and print the list of permissions once user logged in", priority = 3)
	public void validateandPrintPermissionsListOnceUserIsLoggedIn() {
		RestAssured.baseURI = properties.getProperty("LoginURL");
		RestAssured.registerParser("text/plain", Parser.JSON);
		String response = given().log().all().queryParam("email", properties.getProperty("email"))
				.queryParam("password", properties.getProperty("password")).when().get("auth").then().log().all()
				.assertThat().statusCode(SC_OK).body("ok", equalTo(true)).and().contentType(ContentType.JSON).extract()
				.response().asString();

		JsonPath jsonPath = new JsonPath(response);
		int permissionArraySize = jsonPath.getInt("data.permissions.size()");

		// Printing Permissions list inside Auth API, once user is logged in there is list permissions assigned to the user
		
		if (permissionArraySize == 0) {
			Assert.assertTrue(permissionArraySize > 0);
		} else {
			System.out.println("Printing the list of Permissions for User: ");
			for (int i = 0; i < permissionArraySize; i++) {
				System.out.println(jsonPath.getString("data.permissions[" + i + "]"));
			}
		}
	}

	@Test(description = "User should not be able to login without email Id and password", priority = 4)
	public void validateUserIsNotAbletoLoginWithoutEmailIDandPassword() {
		RestAssured.baseURI = properties.getProperty("LoginURL");
		RestAssured.registerParser("text/plain", Parser.JSON);
		String response = given().log().all().when().get("auth").then().log().all().assertThat().statusCode(400)
				.body("ok", equalTo(false)).and().contentType(ContentType.JSON).extract().response().asString();

		JsonPath jsonPath = new JsonPath(response);
		String errorMessage = jsonPath.getString("data.message");
		
		// User should not be able to login if email ID and password are missing.
		// Adding authentication to make sure correct message is received if credentials are missing
		
		Assert.assertEquals(errorMessage, "Invalid login credentials");
	}

	@Test(description = "User should not be able to login with Invalid email Id and password", priority = 5)
	public void validateUserIsNotAbletoLoginWithInvalidEmailIDandPassword() {
		RestAssured.baseURI = properties.getProperty("LoginURL");
		RestAssured.registerParser("text/plain", Parser.JSON);
		String response = given().log().all().queryParam("email", "abc@gmail.com").queryParam("password", "xyz@2023")
				.when().get("auth").then().assertThat().statusCode(400).body("ok", equalTo(false)).and()
				.contentType(ContentType.JSON).extract().response().asString();

		JsonPath jsonPath = new JsonPath(response);
		String errorMessage = jsonPath.getString("data.message");
		
		// User should not able to login with invalid Email ID and password
		// Making sure Invalid message is received in response

		Assert.assertEquals(errorMessage, "Invalid login credentials");
	}

	@Test(description = "User is not able to search without Access Token", priority = 6)
	public void validateUserisnotabletoSearchWithoutAccessToken() {
		RestAssured.baseURI = properties.getProperty("LoginURL");
		RestAssured.registerParser("text/plain", Parser.JSON);
		
		// Access token plays a very important role, in this test scenario access token is removed 
		// and response is received

		String response = given().log().all().when().get("clinics?term=veterinary").then().assertThat().statusCode(401)
				.body("ok", equalTo(false)).and().contentType(ContentType.JSON).extract().response().asString();

		JsonPath jsonPath = new JsonPath(response);
		String message = jsonPath.getString("data.message");

		// Adding validation messaged to check if desired response is received or not
		Assert.assertEquals(message, "You need to be authorized for this action.");
	}

	@Test(description = "Validate Search Keyword using multiple search terms apart from veterinary", priority = 7)
	public void validateForSearchKeyWordUsingMultipleSearchTerms() {
		RestAssured.baseURI = properties.getProperty("LoginURL");
		RestAssured.registerParser("text/plain", Parser.JSON);

		String response = given().queryParam("email", properties.getProperty("email"))
				.queryParam("password", properties.getProperty("password")).when().get("auth").then().assertThat()
				.statusCode(SC_OK).body("ok", equalTo(true)).and().contentType(ContentType.JSON).extract().response()
				.asString();

		JsonPath jsonPath = new JsonPath(response);
		String token = jsonPath.getString("data.session.token");

		Assert.assertNotNull(token);
		
		// Apart from Veterinary search term using the below list to check if response is received or not.
		// If data array in response is empty it will let us know Data array is empty for a specific search term.
		
		String[] searchTerm = { "Dental", "Dermatology", "Vision", "Lasik", "PrimaryCare", "CosmeticSurgery" };
		String response1 = "";
		JsonPath jsonPath1;
		int dataArray = 0;

		for (int i = 0; i < searchTerm.length; i++) {
			response1 = given().header("Authorization", "Bearer " + token).when().get("clinics?term=" + searchTerm[i])
					.then().log().all().assertThat().statusCode(200).body("ok", equalTo(true)).and()
					.contentType(ContentType.JSON).extract().response().asString();

			jsonPath1 = new JsonPath(response1);
			dataArray = jsonPath1.getInt("data.size()");
			
			//If DataArray is greater than zero it will print the search term for which data is present in response
			if (dataArray > 0) {
				System.out.println("Data is retrieved for selected search term: " + searchTerm[i]);

			} else {
				System.out.println("Data is Empty for selected search term: " + searchTerm[i]);
			}
		}
	}
}
