package nl.idgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QueryHandler {

	private static final Logger log = LoggerFactory.getLogger(QueryHandler.class);
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/**
	 * Queries the database to get all data speciefied by the field array and stores them in Lists in a HashMap.
	 * 
	 * @param dbUrl - The table name
	 * @param fields - All column names to get from the database
	 * @param extent - The extent in [xmin, ymin, xmax, ymax]
	 * @param outSR - The spatialRel for output
	 * @param resultOffset - The OFFSET
	 * @param resultRecordCount - The LIMIT
	 * @return All data specified in the fields array.
	 */
	public Map<String, List<String>> getDataFromTable(int layerId, String dbUrl, String[] fields, String where, double[] extent, 
			String outFields, int outSR, int resultOffset, int resultRecordCount, String maxAllowableOffset) {
		log.debug("Connecting to the database...");
		Map<String, List<String>> data = new HashMap<>();
		List<String> list = null;
		
		//String query = createQueryString(maxAllowableOffset, getOutFields(layerId, outFields), dbUrl, getWhereExtent(fields, where, extent, outSR), resultOffset, resultRecordCount, extent, outSR, layerId);
		String query = createQueryString(maxAllowableOffset, outFields, dbUrl, getWhereExtent(fields, where, extent, outSR), resultOffset, resultRecordCount, extent, outSR, layerId);
		
		log.debug("Query: " + query);
		log.debug(jdbcTemplate.getDataSource().toString());
		
		try(Connection conn = jdbcTemplate.getDataSource().getConnection();
			PreparedStatement statement = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
																	   ResultSet.CONCUR_READ_ONLY)) {
			
			log.debug("Connected to the database, getting data...");
			ResultSet rs = statement.executeQuery();
			
			// Check for number of rows
			int numRows = 0;
			if(rs.last()) {
				numRows = rs.getRow();
				rs.beforeFirst();
			}
			
			for(int i = 0; i < fields.length; i++) {
				rs.beforeFirst();
				list = new ArrayList<>(numRows);
				while(rs.next()) {
					list.add(rs.getString(fields[i]));
				}
				data.put(fields[i], list);
			}
			
			log.debug("Got the data from the database...");
			
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
		
		return data;
	}
	
	/**
	 * Creates a SQL query String to send to the database.
	 */
	/*private String createQueryString(String maxAllowableOffset, String outFields, String dbUrl, String whereClause, int resultOffset, int resultRecordCount, double[] extent, int outSR) {
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ST_AsGeoJson(");
		
		if(!"".equals(maxAllowableOffset)) {
			builder.append("ST_SimplifyPreserveTopology(");
		}
		builder.append("\"SHAPE\"");
		if(!"".equals(maxAllowableOffset)) {
			builder.append(",");
			builder.append(Float.parseFloat(maxAllowableOffset));
			builder.append(")");
		}
		
		builder.append(") AS geoJsons, ");
		builder.append(getOutFields(outFields));
		builder.append(" FROM ");
		builder.append(dbUrl);
		builder.append(whereClause);
		builder.append(" OFFSET ");
		builder.append(resultOffset);
		builder.append(" LIMIT ");
		builder.append(resultRecordCount);
		
		return builder.toString();
	}*/
	private String createQueryString(String maxAllowableOffset, String outFields, String dbUrl, String whereClause, 
			int resultOffset, int resultRecordCount, double[] extent, int outSR, int layerId) {
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ST_AsGeoJson(");
		
		if(!"".equals(maxAllowableOffset)) {
			builder.append("ST_SimplifyPreserveTopology(");
		}
		builder.append("single.the_geom");
		if(!"".equals(maxAllowableOffset)) {
			builder.append("," + Float.parseFloat(maxAllowableOffset) + ")");
		}
		builder.append(") AS geoJsons, ");
		/*builder.append(getOutFields(layerId, outFields));
		builder.append(" FROM (SELECT (ST_Dump(avw.\"SHAPE\")).geom AS the_geom, ");
		builder.append(getOutFields(layerId, outFields));
		builder.append(" FROM ");
		builder.append(dbUrl + " as avw ");
		builder.append(whereClause + ") AS single ");*/
		builder.append(getFrom(layerId, dbUrl, outFields, whereClause));
		if(extent.length == 0) {
			builder.append("");
		} else {
			builder.append("WHERE ST_Intersects(single.the_geom, ST_MakeEnvelope(");
			builder.append(extent[0] + "," + extent[1] + "," + extent[2] + "," + extent[3] + "," + outSR + "))");
		}
		builder.append(" OFFSET " + resultOffset);
		builder.append(" LIMIT " + resultRecordCount);
		
		return builder.toString();
	}
	
	private String getFrom(int layerId, String dbUrl, String outFields, String whereClause) {
		StringBuilder builder = new StringBuilder();
		
		if("*".equals(outFields)) {
			String avwOutFields;
			if(layerId == 0) {
				avwOutFields = "\"OBJECTID\", \"CD_VISIE\", \"VERWACHTIN\", \"OMSCHRIJVI\", \"ONDERZOEKS\"";
			} else if (layerId == 1) {
				avwOutFields = "\"OBJECTID\", \"OMS\", \"NR\"";
			} else {
				avwOutFields = "\"OBJECTID\", \"VESTNAAM\", \"STRAATNAAM\", \"HUISNR_TOE\", \"POSTCODE\", \"PLAATSNAAM\", \"GEMEENTENA\", \"TELEFOONNU\", \"HOOFDTYPE\", \"ONDWGEBI_1\", \"COROP_NAAM\", \"WGR_NAAM\"";
			}
			builder.append("* FROM (SELECT (ST_Dump(avw.\"SHAPE\")).geom AS the_geom, " + avwOutFields + " FROM " + dbUrl + " AS avw ");
			builder.append(whereClause + ") AS single ");
			
			return builder.toString();
		}
		
		String[] fields = outFields.split(",");
		builder.append("\"" + fields[0] + "\"");
		if(fields.length > 1) {
			for(int i = 1; i < fields.length; i++) {
				builder.append(",\"" + fields[i] + "\"");
			}
		}
		
		return builder.toString();
	}
	
	
	/**
	 * Gets all columns to return by the query.
	 */
	/*private String getOutFields(int layerId, String outFields) {
		if("*".equals(outFields)) {
			return outFields;
		}
		
		String[] fields = outFields.split(",");
		StringBuilder builder = new StringBuilder();
		builder.append("\"" + fields[0] + "\"");
		
		if(fields.length > 1) {
			for(int i = 1; i < fields.length; i++) {
				builder.append(",\"" + fields[i] + "\"");
			}
		}
		
		
		return builder.toString();
	}*/
	
	/**
	 * Gets the geometries within the given bounding box and extends with the where clause
	 * 
	 * @param where - The WHERE clause
	 * @param extent - The bounding box in which the geometries to get
	 * @param outSR - The spatialRel
	 * @return Returns the WHERE string
	 */
	private String getWhereExtent(String[] fields, String where, double[] extent, int outSR) {
		if(extent.length == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append(" WHERE ST_Intersects(\"SHAPE\", ST_MakeEnvelope(" + extent[0] + ", " + extent[1] + ", "
															   + extent[2] + ", " + extent[3] + ", " 
															   + outSR + "))");
		if(!"".equals(where)) {
			builder.append(" AND ");
			builder.append(parseWhere(fields, where));
		}
		
		return builder.toString();
	}
	
	private String parseWhere(String[] fields, String where) {
		log.debug("Where: " + where);
		
		for(String field : fields) {
			where = where.replace(field, "\"" + field + "\"");
		}
		
		log.debug("Return where: " + where);
		return where;
	}
}
