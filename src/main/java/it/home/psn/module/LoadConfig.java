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
	
	public synchronized static LoadConfig getInstance() {
		return SINGLETON;
	}
	
	private LoadConfig() {
		try {
			load(config, "config.properties");
			load(ricerche, "ricerche.properties");
			load(preferiti, "preferiti.properties");
			load(posseduti, "posseduti-digitale.properties");
			load(posseduti, "posseduti-fisico.properties");

		
			for(final Object urlObj : this.posseduti.values()) {
				final String targetUrl = this.config.getProperty("base.html.url");
				idPosseduti.add(urlObj.toString().replace(targetUrl, ""));
			}
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
		final Set<CoppiaUrl> urls = new HashSet<>();
		for(final Object urlObj : this.preferiti.values()) {
			urls.add(create(urlObj));
		}
		for(final Object urlObj : this.posseduti.values()) {
			urls.add(create(urlObj));
		}
		System.err.println("Urls da analizzare " + urls.size());
		System.err.println("Giochi posseduti " + this.posseduti.size());
		return urls;
	}
	
	private CoppiaUrl create(final Object urlObj) {
		final String jsonUrl = this.config.getProperty("base.json.url");
		final String targetUrl = this.config.getProperty("base.html.url");
		final String url = urlObj.toString();
		return new CoppiaUrl(url, url.replace(targetUrl, jsonUrl));
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
	private final Properties posseduti = new Properties();
	private final Properties config = new Properties();
	private final Properties ricerche = new Properties();
	private final Set<String> idPosseduti = Utils.createSet();
	
	@Data
	public static class CoppiaUrl{
		private final String originUrl;
		private final String jsonUrl;
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((originUrl == null) ? 0 : originUrl.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CoppiaUrl other = (CoppiaUrl) obj;
			if (originUrl == null) {
				if (other.originUrl != null)
					return false;
			} else if (!originUrl.equals(other.originUrl))
				return false;
			return true;
		}
	}
}
