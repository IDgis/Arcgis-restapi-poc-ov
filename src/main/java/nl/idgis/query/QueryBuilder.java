package nl.idgis.query;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.esri.terraformer.core.Terraformer;
import com.esri.terraformer.core.TerraformerException;
import com.esri.terraformer.formats.EsriJson;
import com.esri.terraformer.formats.GeoJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.idgis.QueryHandler;

@Component
public class QueryBuilder {
	
	private static final String ARCHEOLOGISCH = "staging_data.\"d43498d0-e418-44fe-b5ca-7635d7770e2a\"";
	//private static final String ARCHEOLOGISCH = "staging_data.\"testdata\"";
	private static final String BOOMKIKKERS = "staging_data.\"12e0f00d-cbea-4517-bc5d-97cb0828419e\"";
	private static final String ONDERWIJS = "staging_data.\"7cdb24f1-7fcd-45df-83d5-ec2c7b86e355\"";
	
	private static final String[] archeologischFields = {"OBJECTID", "CD_VISIE", "VERWACHTIN", "OMSCHRIJVI", "ONDERZOEKS", "geoJsons"};
	private static final String[] boomkikkersFields = {"OBJECTID", "OMS", "NR", "geoJsons"};
	private static final String[] onderwijsFields = {"OBJECTID", "VESTNAAM", "STRAATNAAM", "HUISNR_TOE", "POSTCODE", "PLAATSNAAM", "GEMEENTENA", "TELEFOONNU", "HOOFDTYPE", "ONDWGEBI_1", "COROP_NAAM", "WGR_NAAM", "geoJsons"};
	
	private static final Logger log = LoggerFactory.getLogger(QueryBuilder.class);
	
	@Autowired 
	private QueryHandler handler;

	/**
	 * Builds the json to return to ArcGIS so the results can be displayed on the map.
	 * 
	 * @param layerId - The layer number
	 * @return
	 */
	public String getJsonQueryResult(int layerId, String where, boolean returnGeometry, String maxAllowableOffset, String geometry, 
			String outFields, int outSR, int resultOffset, int resultRecordCount) {
		log.debug("Generating data...");
		String dbUrl = getDbUrl(layerId);
		String[] fields = getFieldsToGet(layerId, outFields);
		
		log.debug("Fields to filter: ");
		for(String field : fields) {
			log.debug(field);
		}
		
		double[] extent = getExtentFromGeometry(geometry);
		Map<String, List<String>> data = handler.getDataFromTable(layerId, dbUrl, fields, where, extent, outFields, outSR, resultOffset, resultRecordCount, maxAllowableOffset);
		
		JsonObject obj = new JsonObject();
		
		obj.addProperty("objectIdFieldName", "OBJECTID");
		obj.addProperty("globalIdFieldName", "");
		if(layerId == 2) {
			obj.addProperty("geometryType", "esriGeometryPoint");
		} else {
			obj.addProperty("geometryType", "esriGeometryPolygon");
		}
		obj.add("spatialReference", getSpatialReference());
		obj.add("fields", getFields(layerId));
		obj.add("features", getFeatures(data, returnGeometry, fields));
		
		return obj.toString();
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Gets the name of the table to search
	 * 
	 * @param layerId - The layer id
	 * @return The table name
	 */
	private String getDbUrl(int layerId) {
		switch(layerId) {
		case 0:
			return ARCHEOLOGISCH;
		case 1:
			return BOOMKIKKERS;
		case 2:
			return ONDERWIJS;
		default:
			return null;
		}
	}
	
	/**
	 * Gets all fields from the layer metadata. These are the column names to get from the database table
	 * 
	 * @param layerId - The layer id
	 * @return The names of the columns to filter
	 */
	private String[] getFieldsToGet(int layerId, String outFields) {
		if("*".equals(outFields)) {
			switch(layerId) {
			case 0:
				return archeologischFields;
			case 1:
				return boomkikkersFields;
			default:
				return onderwijsFields;
			}
		}
		
		StringBuilder builder = new StringBuilder();
		String[] fields = outFields.split(",");
		builder.append(fields[0]);
		if(fields.length > 1) {
			for(int i = 1; i < fields.length; i++) {
				builder.append("," + fields[i]);
			}
		}
		builder.append(",geoJsons");
		log.debug("outFields: " + builder.toString());
		return builder.toString().split(",");
	}
	
	/**
	 * Gets the extent from the geometry attribute
	 * 
	 * @param geometry - The geometry attribute
	 * @return Return the extent as a double[]
	 */
	private double[] getExtentFromGeometry(String geometry) {
		if("".equals(geometry)) {
			return new double[0];
		}
		
		JsonParser parser = new JsonParser();
		JsonElement element = parser.parse(geometry);
		
		double xmin = element.getAsJsonObject().get("xmin").getAsDouble();
		double ymin = element.getAsJsonObject().get("ymin").getAsDouble();
		double xmax = element.getAsJsonObject().get("xmax").getAsDouble();
		double ymax = element.getAsJsonObject().get("ymax").getAsDouble();
		
		return new double[]{ xmin, ymin, xmax, ymax };
	}
	
	/**
	 * Gets the spatialRel
	 * 
	 * @return The spatialRel
	 */
	private JsonObject getSpatialReference() {
		log.debug("Getting spatialReference...");
		JsonObject obj = new JsonObject();
		
		obj.addProperty("wkid", 28992);
		obj.addProperty("latestWkid", 28992);
		
		return obj;
	}
	
	/**
	 * Gets the column names to filter and displays them within the field array.
	 * 
	 * @param layerId - The layer id
	 * @return The fields as a JsonArray
	 */
	private JsonArray getFields(int layerId) {
		log.debug("Getting fields...");
		
		if(layerId == 0) {
			return getArcheologischeFields();
		}
		else if(layerId == 1) {
			return getBoomkikkerFields();
		}
		else {
			return getOnderwijsFields();
		}
	}
	
	/**
	 * Hard-coded fields for the Layer Archeologische Verwachtingenkaart.
	 * 
	 * @return
	 */
	private JsonArray getArcheologischeFields() {
		JsonArray arr = new JsonArray();
		
		JsonObject objectId = new JsonObject();
		objectId.addProperty("name", "OBJECTID");
		objectId.addProperty("type", "esriFieldTypeOID");
		objectId.addProperty("alias", "OBJECTID");
		objectId.addProperty("sqlType", "sqlTypeOther");
		objectId.add("domain", null);
		objectId.add("defaultValue", null);
		
		JsonObject cdVisie = new JsonObject();
		cdVisie.addProperty("name", "CD_VISIE");
		cdVisie.addProperty("type", "esriFieldTypeInteger");
		cdVisie.addProperty("alias", "CD_VISIE");
		cdVisie.addProperty("sqlType", "sqlTypeOther");
		cdVisie.add("domain", null);
		cdVisie.add("defaultValue", null);
		
		JsonObject verwachting = new JsonObject();
		verwachting.addProperty("name", "VERWACHTIN");
		verwachting.addProperty("type", "esriFieldTypeString");
		verwachting.addProperty("alias", "VERWACHTIN");
		verwachting.addProperty("sqlType", "sqlTypeOther");
		verwachting.addProperty("length", 200);
		verwachting.add("domain", null);
		verwachting.add("defaultValue", null);
		
		JsonObject omschrijving = new JsonObject();
		omschrijving.addProperty("name", "OMSCHRIJVI");
		omschrijving.addProperty("type", "esriFieldTypeString");
		omschrijving.addProperty("alias", "OMSCHRIJVI");
		omschrijving.addProperty("sqlType", "sqlTypeOther");
		omschrijving.addProperty("length", 200);
		omschrijving.add("domain", null);
		omschrijving.add("defaultValue", null);
		
		JsonObject onderzoeks = new JsonObject();
		onderzoeks.addProperty("name", "ONDERZOEKS");
		onderzoeks.addProperty("type", "esriFieldTypeString");
		onderzoeks.addProperty("alias", "ONDERZOEKS");
		onderzoeks.addProperty("sqlType", "sqlTypeOther");
		onderzoeks.addProperty("length", 200);
		onderzoeks.add("domain", null);
		onderzoeks.add("defaultValue", null);
		
		arr.add(objectId);
		arr.add(cdVisie);
		arr.add(verwachting);
		arr.add(omschrijving);
		arr.add(onderzoeks);
		return arr;
	}
	
	/**
	 * Hard-coded fields for the Layer Beschermingsplan Boomkikkers
	 * 
	 * @return
	 */
	private JsonArray getBoomkikkerFields() {
		JsonArray arr = new JsonArray();
		
		JsonObject oms = new JsonObject();
		oms.addProperty("name", "OMS");
		oms.addProperty("type", "esriFieldTypeString");
		oms.addProperty("alias", "OMS");
		oms.addProperty("sqlType", "sqlTypeOther");
		oms.addProperty("length", 200);
		oms.add("domain", null);
		oms.add("defaultValue", null);
		
		JsonObject nr = new JsonObject();
		nr.addProperty("name", "NR");
		nr.addProperty("type", "esriFieldTypeInteger");
		nr.addProperty("alias", "NR");
		nr.addProperty("sqlType", "sqlTypeOther");
		nr.add("domain", null);
		nr.add("defaultValue", null);
		
		arr.add(oms);
		arr.add(nr);
		return arr;
	}
	
	/**
	 * Hard-coded fields for the Layer Onderwijsinstellingen
	 * 
	 * @return
	 */
	private JsonArray getOnderwijsFields() {
		JsonArray arr = new JsonArray();
		
		JsonObject vestNaam = new JsonObject();
		vestNaam.addProperty("name", "VESTNAAM");
		vestNaam.addProperty("type", "esriFieldTypeString");
		vestNaam.addProperty("alias", "VESTNAAM");
		vestNaam.addProperty("sqlType", "sqlTypeOther");
		vestNaam.addProperty("length", 200);
		vestNaam.add("domain", null);
		vestNaam.add("defaultValue", null);
		
		JsonObject straatNaam = new JsonObject();
		straatNaam.addProperty("name", "STRAATNAAM");
		straatNaam.addProperty("type", "esriFieldTypeString");
		straatNaam.addProperty("alias", "STRAATNAAM");
		straatNaam.addProperty("sqlType", "sqlTypeOther");
		straatNaam.addProperty("length", 200);
		straatNaam.add("domain", null);
		straatNaam.add("defaultValue", null);
		
		JsonObject huisnr = new JsonObject();
		huisnr.addProperty("name", "HUISNR_TOE");
		huisnr.addProperty("type", "esriFieldTypeString");
		huisnr.addProperty("alias", "HUISNR_TOE");
		huisnr.addProperty("sqlType", "sqlTypeOther");
		huisnr.addProperty("length", 200);
		huisnr.add("domain", null);
		huisnr.add("defaultValue", null);
		
		JsonObject postcode = new JsonObject();
		postcode.addProperty("name", "POSTCODE");
		postcode.addProperty("type", "esriFieldTypeString");
		postcode.addProperty("alias", "POSTCODE");
		postcode.addProperty("sqlType", "sqlTypeOther");
		postcode.addProperty("length", 200);
		postcode.add("domain", null);
		postcode.add("defaultValue", null);
		
		JsonObject plaatsnaam = new JsonObject();
		plaatsnaam.addProperty("name", "PLAATSNAAM");
		plaatsnaam.addProperty("type", "esriFieldTypeString");
		plaatsnaam.addProperty("alias", "PLAATSNAAM");
		plaatsnaam.addProperty("sqlType", "sqlTypeOther");
		plaatsnaam.addProperty("length", 200);
		plaatsnaam.add("domain", null);
		plaatsnaam.add("defaultValue", null);
		
		JsonObject gemeentenaam = new JsonObject();
		gemeentenaam.addProperty("name", "GEMEENTENA");
		gemeentenaam.addProperty("type", "esriFieldTypeString");
		gemeentenaam.addProperty("alias", "GEMEENTENA");
		gemeentenaam.addProperty("sqlType", "sqlTypeOther");
		gemeentenaam.addProperty("length", 200);
		gemeentenaam.add("domain", null);
		gemeentenaam.add("defaultValue", null);
		
		JsonObject telefoon = new JsonObject();
		telefoon.addProperty("name", "TELEFOONNU");
		telefoon.addProperty("type", "esriFieldTypeString");
		telefoon.addProperty("alias", "TELEFOONNU");
		telefoon.addProperty("sqlType", "sqlTypeOther");
		telefoon.addProperty("length", 200);
		telefoon.add("domain", null);
		telefoon.add("defaultValue", null);
		
		JsonObject hoofdtype = new JsonObject();
		hoofdtype.addProperty("name", "HOOFDTYPE");
		hoofdtype.addProperty("type", "esriFieldTypeString");
		hoofdtype.addProperty("alias", "HOOFDTYPE");
		hoofdtype.addProperty("sqlType", "sqlTypeOther");
		hoofdtype.addProperty("length", 200);
		hoofdtype.add("domain", null);
		hoofdtype.add("defaultValue", null);
		
		JsonObject gebied = new JsonObject();
		gebied.addProperty("name", "ONDWGEBI_1");
		gebied.addProperty("type", "esriFieldTypeString");
		gebied.addProperty("alias", "ONDWGEBI_1");
		gebied.addProperty("sqlType", "sqlTypeOther");
		gebied.addProperty("length", 200);
		gebied.add("domain", null);
		gebied.add("defaultValue", null);
		
		JsonObject corop = new JsonObject();
		corop.addProperty("name", "COROP_NAAM");
		corop.addProperty("type", "esriFieldTypeString");
		corop.addProperty("alias", "COROP_NAAM");
		corop.addProperty("sqlType", "sqlTypeOther");
		corop.addProperty("length", 200);
		corop.add("domain", null);
		corop.add("defaultValue", null);
		
		JsonObject wgr = new JsonObject();
		wgr.addProperty("name", "WGR_NAAM");
		wgr.addProperty("type", "esriFieldTypeString");
		wgr.addProperty("alias", "WGR_NAAM");
		wgr.addProperty("sqlType", "sqlTypeOther");
		wgr.addProperty("length", 200);
		wgr.add("domain", null);
		wgr.add("defaultValue", null);
		
		arr.add(vestNaam);
		arr.add(straatNaam);
		arr.add(huisnr);
		arr.add(postcode);
		arr.add(plaatsnaam);
		arr.add(gemeentenaam);
		arr.add(telefoon);
		arr.add(hoofdtype);
		arr.add(gebied);
		arr.add(corop);
		arr.add(wgr);
		return arr;
	}
	
	/**
	 * Gets all features as an JsonArray for the given table
	 * 
	 * @param data - All the filtered data from the database
	 * @param returnGeometry - A boolean whether the geometries should be returned
	 * @param fields - The filtered column names
	 * @return
	 */
	private JsonArray getFeatures(Map<String, List<String>> data, boolean returnGeometry, String[] fields) {
		log.debug("Getting features...");
		JsonArray arr = new JsonArray();
		
		int numObjects = (data.get("geoJsons")).size();
		log.debug(String.format("%d results found...", numObjects));
		
		if(numObjects > 0) {
			for(int i = 0; i < numObjects; i++) {
				arr.add(getFeature(data, i, returnGeometry, fields));
			}
		}
		
		return arr;
	}
	
	/**
	 * Gets each feature as a JsonObject and appends it to the rest of the metadata
	 * 
	 * @param data - All filtered data from the database
	 * @param index - This feature number
	 * @param returnGeometry - Boolean whether the geometries should be returned
	 * @param fields - The filtered column names
	 * @return
	 */
	private JsonObject getFeature(Map<String, List<String>> data, int index, boolean returnGeometry, String[] fields) {
		JsonObject obj = new JsonObject();
		
		obj.add("attributes", getAttributes(data, index, fields));
		
		List<String> geoJsons = data.get("geoJsons");
		String esriJson = getEsriJson(geoJsons.get(index));
		JsonParser parser = new JsonParser();
		
		if(returnGeometry) {
			obj.add("geometry", parser.parse(esriJson));
		}
		
		return obj;
	}
	
	/**
	 * Gets all attributes for the current feature
	 * 
	 * @param data - All filtered data from the database
	 * @param index - The number of the current feature
	 * @param fields - The filtered column names
	 * @return
	 */
	private JsonObject getAttributes(Map<String, List<String>> data, int index, String[] fields) {
		JsonObject obj = new JsonObject();
		
		for(int i = 0; i < fields.length; i++) {
			String field = fields[i];
			if("geoJsons".equalsIgnoreCase(field)) {
				continue;
			}
			
			String listObj = data.get(field).get(index);
			obj.addProperty(field, listObj);
		}
		
		return obj;
	}
	
	/**
	 * Converts the GeoJson String to an EsriJson String
	 * 
	 * @param geoJson - The GeoJson String
	 * @return
	 */
	private String getEsriJson(String geoJson) {
		Terraformer t = new Terraformer();
		
		t.setDecoder(new GeoJson());
		EsriJson ej = new EsriJson();
		ej.setSpatialReference(28992);
		t.setEncoder(ej);
		
		String esriJson = "";
		try {
			esriJson = t.convert(geoJson);
		} catch(TerraformerException e) {
			log.error(e.getMessage(), e);
		}
		
		return esriJson;
	}
}