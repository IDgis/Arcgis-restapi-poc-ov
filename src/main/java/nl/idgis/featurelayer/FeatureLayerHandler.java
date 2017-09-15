package nl.idgis.featurelayer;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

import nl.idgis.ErrorMessageHandler;
import nl.idgis.MetaDataHandler;

public class FeatureLayerHandler implements MetaDataHandler {

	private static final Logger log = LoggerFactory.getLogger(FeatureLayerHandler.class); 
	
	public FeatureLayerHandler() {
		super();
		log.debug("Creating FeatureLayerHandler...");
	}
	
	public Map<String, Object> getMetadata(String fileUrl) {
		log.info("Getting metadata for feature layer...");
		String fileContent = null;
		try {
			fileContent = readFile(fileUrl);
		} catch(IOException e) {
			log.error(e.getMessage(), e);
			return ErrorMessageHandler.getErrorMessage(String.format("Could not open or read the file from: %s", fileUrl));
		}
		
		JsonParser parser = JsonParserFactory.getJsonParser();
		return parser.parseMap(fileContent);
	}
}
