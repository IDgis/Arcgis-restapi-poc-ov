package nl.idgis;

import java.util.HashMap;
import java.util.Map;

public class ErrorMessageHandler {
	
	private ErrorMessageHandler() {}

	public static Map<String, Object> getErrorMessage(String message) {
		Map<String, Object> obj = new HashMap<>();
		
		obj.put("error", message);
		
		return obj;
	}
}
