package nl.idgis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@FunctionalInterface
public interface MetaDataHandler {
	
	public Map<String, Object> getMetadata(String fileUrl);
	
	default public String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Charset.defaultCharset());
	}
}
