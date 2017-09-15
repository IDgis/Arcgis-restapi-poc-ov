package nl.idgis.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nl.idgis.ErrorMessageHandler;
import nl.idgis.MetaDataHandler;
import nl.idgis.featurelayer.FeatureLayerHandler;
import nl.idgis.featureserver.FeatureServerHandler;
import nl.idgis.query.QueryBuilder;

@RestController
@RequestMapping("/rest")
public class Controller {
	
	private static final Logger log = LoggerFactory.getLogger(Controller.class);
	
	private static final String FORMAT_ERROR_MESSAGE = "Invalid format type. Can only return JSON!";
	
	@Autowired
	private QueryBuilder builder;
	
	/**
	 * The ServerInfo resource provides general information about the server (e.g. current version of the server), 
	 * and provides information on whether the server is secured using token based authentication; and the token 
	 * services url (if token based authentication is used).
	 * 
	 * @param formatType
	 * @return
	 */
	@RequestMapping("/info")
	public ResponseEntity<Map<String, Object>> getServerInfo(
			@RequestParam(value="f", defaultValue="json") String formatType) {
		
		if(!"json".equalsIgnoreCase(formatType)) {
			log.warn(FORMAT_ERROR_MESSAGE);
			return new ResponseEntity<>(ErrorMessageHandler.getErrorMessage(FORMAT_ERROR_MESSAGE), HttpStatus.BAD_REQUEST);
		}
		
		JsonObject obj = new JsonObject();
		obj.addProperty("currentVersion", 10.51);
		obj.addProperty("owningSystemUrl", "http://localhost:8080");
		
		JsonObject authInfo = new JsonObject();
		obj.addProperty("isTokenBasedSecurity", false);
		obj.add("authInfo", authInfo);
		
		JsonParser parser = JsonParserFactory.getJsonParser();
		return new ResponseEntity<>(parser.parseMap(obj.toString()), HttpStatus.OK);
	}
	
	@RequestMapping("/services")
	public ResponseEntity<Map<String, Object>> getServices(
			@RequestParam(value="f", defaultValue="json") String formatType) {
		
		if(!"json".equalsIgnoreCase(formatType)) {
			log.warn(FORMAT_ERROR_MESSAGE);
			return new ResponseEntity<>(ErrorMessageHandler.getErrorMessage(FORMAT_ERROR_MESSAGE), HttpStatus.BAD_REQUEST);
		}
		
		JsonObject obj = new JsonObject();
		obj.addProperty("version", 10.51);
		
		JsonArray services = new JsonArray();
		
		JsonObject service = new JsonObject();
		
		service.addProperty("name", "Testdata");
		service.addProperty("type", "FeatureServer");
		service.addProperty("url", "http://localhost:8080/rest/services/Testdata/FeatureServer");
		services.add(service);
		
		obj.add("services", services);
		
		JsonParser parser = JsonParserFactory.getJsonParser();
		return new ResponseEntity<>(parser.parseMap(obj.toString()), HttpStatus.OK);
	}

	/**
	 * This mapping gets the metadata for the FeatureServer. If an invalid format type is given, it will give
	 * an error message in json.
	 * @param serviceName - The service name
	 * @param formatType - The return type for the metadata. Only json is available.
	 * @return The metadata of the FeatureServer in JSON.
	 */
	@RequestMapping("/services/{serviceName}/FeatureServer")
	public ResponseEntity<Map<String, Object>> getFeatureServerMetadata(
			@PathVariable String serviceName,
			@RequestParam(value="f", defaultValue="json") String formatType) {
		
		if(!"json".equalsIgnoreCase(formatType)) {
			log.warn(FORMAT_ERROR_MESSAGE);
			return new ResponseEntity<>(ErrorMessageHandler.getErrorMessage(FORMAT_ERROR_MESSAGE), HttpStatus.BAD_REQUEST);
		}
		
		MetaDataHandler metaDataHandler = new FeatureServerHandler();
		log.debug(String.format("Getting metadata for serviceName: %s", serviceName));
		return new ResponseEntity<>(metaDataHandler.getMetadata("./examples/featureserver.json"), HttpStatus.OK);
	}
	
	/**
	 * This mapping gets the metadata for the FeatureLayer. If an invalid format type is given, it will give
	 * an error message in json.
	 * @param serviceName - The service name
	 * @param layerId - The layer id for the given service name. Only layers 0 and 1 are available.
	 * @param formatType - The return type for the metadata. Only json is available.
	 * @return The metadata of the FeatureLayer in JSON.
	 */
	@RequestMapping("/services/{serviceName}/FeatureServer/{layerId}")
	public ResponseEntity<Map<String, Object>> getFeatureLayerMetadata(
			@PathVariable String serviceName,
			@PathVariable int layerId,
			@RequestParam(value="f", defaultValue="json") String formatType) {
		
		log.info("Got a request to get metadata for FeatureLayer...");
		
		if(!"json".equalsIgnoreCase(formatType)) {
			log.warn(FORMAT_ERROR_MESSAGE);
			return new ResponseEntity<>(ErrorMessageHandler.getErrorMessage(FORMAT_ERROR_MESSAGE), HttpStatus.BAD_REQUEST);
		}
		
		String jsonUrl = null;
		if(layerId == 0) {
			jsonUrl = "./examples/featureLayer0.json";
		}
		else if(layerId == 1) {
			jsonUrl = "./examples/featureLayer1.json";
		} else {
			jsonUrl = "./examples/featureLayer2.json";
		}
		
		MetaDataHandler metaDataHandler = new FeatureLayerHandler();
		log.debug(String.format("Getting metadata for serviceName: %s and layerId: %d", serviceName, layerId));
		return new ResponseEntity<>(metaDataHandler.getMetadata(jsonUrl), HttpStatus.OK);
	}
	
	/**
	 * This mapping gets the metadata for the query with the given parameters.
	 * @param serviceName - The service name
	 * @param layerId - The layer id for the given service name. Only layers 0 and 1 are available.
	 * @param formatType - The response format. The default response format is html.
	 * @param where - A where clause for the query filter. Any legal SQL where clause operating on the fields in the layer is allowed.
	 * @param returnGeometry - If true, the resultset includes the geometry associated with each result. The default is true.
	 * @param geometry - The extent of the geometry to get from the database.
	 * @param outFields -  The list of fields to be included in the returned resultset. This list is a comma delimited 
	 * 		list of field names. If you specify the shape field in the list of return fields, it is ignored. To request geometry, 
	 * 		set returnGeometry to true. You can also specify the wildcard "*" as the value of this parameter. In this case, the 
	 * 		query results include all the field values.
	 * @param resultOffset - Description: This option can be used for fetching query results by skipping the specified number 
	 * 		of records and starting from the next record (that is, resultOffset + 1th). The default is 0. This parameter only 
	 * 		applies if supportsPagination is true. You can use this option to fetch records that are beyond maxRecordCount. 
	 * 		For example, if maxRecordCount is 1000, you can get the next 100 records by setting resultOffset=1000 and 
	 * 		resultRecordCount = 100, so the query results will return the results in the range of 1001 to 1100.
	 * @param resultRecordCount - Description: This option can be used for fetching query results up to the resultRecordCount specified. 
	 * 		When resultOffset is specified but this parameter is not, the map service defaults it to maxRecordCount. 
	 * 		The maximum value for this parameter is the value of the layer's maxRecordCount property. This parameter only applies if 
	 * 		supportsPagination is true. Example: resultRecordCount=10 to fetch up to 10 records
	 * @return The metadata for the specified query in JSON
	 */
	@RequestMapping("/services/{serviceName}/FeatureServer/{layerId}/query")
	public ResponseEntity<Map<String, Object>> getQueryResult(
			@PathVariable String serviceName,
			@PathVariable int layerId,
			@RequestParam(value="f", defaultValue="json") String formatType,
			@RequestParam(value="where", defaultValue="") String where,
			@RequestParam(value="returnGeometry", defaultValue="true") boolean returnGeometry,
			@RequestParam(value="maxAllowableOffset", defaultValue="") String maxAllowableOffset,
			@RequestParam(value="geometry", defaultValue="") String geometry,
			@RequestParam(value="outFields", defaultValue="*") String outFields,
			@RequestParam(value="outSR", defaultValue="28992") int outSR,
			@RequestParam(value="resultOffset", defaultValue="0") int resultOffset,
			@RequestParam(value="resultRecordCount", defaultValue="1000") int resultRecordCount) {
		
		log.debug(String.format("Got a query request for layer %d, getting data...", layerId));
		
		// Check for required fields to be present
		if(!"json".equalsIgnoreCase(formatType)) {
			log.warn(FORMAT_ERROR_MESSAGE);
			return new ResponseEntity<>(ErrorMessageHandler.getErrorMessage(FORMAT_ERROR_MESSAGE), HttpStatus.BAD_REQUEST);
		}
		
		String retVal = builder.getJsonQueryResult(layerId, where, returnGeometry, maxAllowableOffset, geometry, outFields, outSR, resultOffset, resultRecordCount);
		
		log.debug("Got the data, returning the result...");
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("private, max-age=86400");
		JsonParser parser = JsonParserFactory.getJsonParser();
		return new ResponseEntity<>(parser.parseMap(retVal), headers, HttpStatus.OK);
	}
}
