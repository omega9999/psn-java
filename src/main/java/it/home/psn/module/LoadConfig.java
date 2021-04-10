package it.home.psn.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import it.home.psn.Utils;
import lombok.Data;
import lombok.Getter;

@Getter
public class LoadConfig {
	
	private static LoadConfig SINGLETON = new LoadConfig();
	
	public static LoadConfig getInstance() {
		return SINGLETON;
	}
	
	private LoadConfig() {
		try {
			load(preferiti, "preferiti.properties");
			load(config, "config.properties");
			load(ricerche, "ricerche.properties");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Set<String> getRicercheIds(){
		final Set<String> res = Utils.createSet();
		for(String key : this.ricerche.stringPropertyNames()) {
			res.add(this.ricerche.getProperty(key));
		}
		return res;
	}
	
	public Set<CoppiaUrl> getUrls(){
		final String jsonUrl = this.config.getProperty("base.json.url");
		final String targetUrl = this.config.getProperty("base.html.url");
		final Set<CoppiaUrl> urls = new HashSet<>();
		for(final Object urlObj : this.preferiti.values()) {
			final String url = urlObj.toString();
			urls.add(new CoppiaUrl(url, url.replace(targetUrl, jsonUrl)));
		}
		return urls;
	}
	
	public static CoppiaUrl getCoppia(String id) {
		final String jsonUrl = getInstance().config.getProperty("base.json.url");
		final String targetUrl = getInstance().config.getProperty("base.html.url");
		return new CoppiaUrl(targetUrl+id, jsonUrl+id);
	}


	public void checkPreferiti() throws IOException {
		for(final Entry<Object, Object> entry : this.preferiti.entrySet()) {
			final String key = entry.getKey().toString();
			final String value = entry.getValue().toString();
			if (!value.startsWith("http")) {
				throw new IOException(String.format("La chiave '%1$s' non ha un url: %2$s", key, value));
			}
		}
	}
	
	private void load(final Properties prop, final String fileName) throws IOException {
		final ClassLoader classLoader = getClass().getClassLoader();
		final InputStream inStream = classLoader.getResourceAsStream(fileName);
		prop.load(inStream);
	}
	
	
	
	private final Properties preferiti = new Properties();
	private final Properties config = new Properties();
	private final Properties ricerche = new Properties();
	
	@Data
	public static class CoppiaUrl{
		private final String originUrl;
		private final String jsonUrl;
	}
}
