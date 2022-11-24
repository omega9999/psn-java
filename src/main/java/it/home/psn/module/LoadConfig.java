package it.home.psn.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import it.home.psn.SistemaPreferiti;
import it.home.psn.Utils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;

@Log4j
@Getter
public class LoadConfig {

	private static final LoadConfig SINGLETON = new LoadConfig();
	static {
		try {
			load(getInstance().config, "config.properties");
			load(getInstance().ricerche, "ricerche.properties");
			
			loadProp(getInstance().gameGenre, "decode/game_genre.properties");
			loadProp(getInstance().gameSubgenre, "decode/game_subgenre.properties");
			loadProp(getInstance().primaryClassification, "decode/primary_classification.properties");
			loadProp(getInstance().secondaryClassification, "decode/secondary_classification.properties");
			loadProp(getInstance().tertiaryClassification, "decode/tertiary_classification.properties");
			
			
			load(getInstance().preferiti, new File(SistemaPreferiti.root, "preferiti.properties"));
			load(getInstance().preferiti, new File(SistemaPreferiti.root, "posseduti-demo.properties"));
			load(getInstance().preferiti, new File(SistemaPreferiti.root, "posseduti-digitale.properties"));
			load(getInstance().preferiti, new File(SistemaPreferiti.root, "posseduti-fisico.properties"));

			load(getInstance().posseduti, new File(SistemaPreferiti.root, "posseduti-digitale.properties"));
			load(getInstance().posseduti, new File(SistemaPreferiti.root, "posseduti-fisico.properties"));

			for (final CoppiaUrl urlObj : getInstance().posseduti.values()) {
				getInstance().idPosseduti.add(urlObj.getId());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static synchronized LoadConfig getInstance() {
		return SINGLETON;
	}

	private LoadConfig() {

	}

	public Set<String> getRicercheIds() {
		final Set<String> res = Utils.createSet();
		for (String key : this.ricerche.stringPropertyNames()) {
			res.add(this.ricerche.getProperty(key));
		}
		return res;
	}

	public Set<CoppiaUrl> getUrls() {
		final Set<CoppiaUrl> urls = new HashSet<>();
		for (final CoppiaUrl urlObj : this.preferiti.values()) {
			urls.add(urlObj);
		}
		for (final CoppiaUrl urlObj : this.posseduti.values()) {
			urls.add(urlObj);
		}
		log.info("Urls da analizzare " + urls.size());
		log.info("Giochi posseduti " + this.posseduti.size());
		return urls;
	}

	public static CoppiaUrl create(final String urlObj) {
		final String[] str = urlObj.split("\\?");
		String s = str[0].trim();
		s = s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
		final String[] ss = s.split("/");
		final String id = ss[ss.length - 1];
		return getCoppia(id);
	}

	public static CoppiaUrl getCoppia(String id) {
		final String sha256Hash1 = getInstance().config.getProperty("sha256Hash1");
		final String sha256Hash2 = getInstance().config.getProperty("sha256Hash2");
		final String sha256Hash3 = getInstance().config.getProperty("sha256Hash3");
		final String jsonUrl = replace(getInstance().config.getProperty("base.json.url"), "id", id);
		final String targetUrl = replace(getInstance().config.getProperty("base.html.url"), "id", id);
		final String priceUrl = replace(replace(getInstance().config.getProperty("base.json.price.url"), "id", id), "sha256Hash1", sha256Hash1);
		final String addictionalUrl = replace(replace(getInstance().config.getProperty("base.json.addictional-price.url"), "id", id), "sha256Hash2", sha256Hash2);
		final String imageUrl = replace(replace(getInstance().config.getProperty("base.json.addictional-image.url"), "id", id), "sha256Hash3", sha256Hash3);
		return new CoppiaUrl(id, targetUrl, jsonUrl, priceUrl, addictionalUrl, imageUrl);
	}
	
	public static String replace(String target, String label, String value) {
		return target.replace("{" + label + "}", value);
	}

	public static void checkPreferiti(final Properties prop) throws IOException {
		for (final Entry<Object, Object> entry : prop.entrySet()) {
			final String key = entry.getKey().toString();
			final String value = entry.getValue().toString();
			if (!value.startsWith("http")) {
				throw new IOException(String.format("La chiave '%1$s' non ha un url: %2$s", key, value));
			}
		}
	}

	private static void load(final Properties prop, final String fileName) throws IOException {
		final ClassLoader classLoader = LoadConfig.class.getClassLoader();
		try (final InputStream inStream = classLoader.getResourceAsStream(fileName)) {
			prop.load(inStream);
		}
	}

	private static void load(final Map<String, CoppiaUrl> urls, final File file) throws IOException {
		final MyProperties prop = new MyProperties();
		try (final InputStream inStream = FileUtils.openInputStream(file)) {
			prop.load(inStream);
		}
		checkPreferiti(prop);
		for (final String urlObj : prop.valuesAsString()) {
			CoppiaUrl create = create(urlObj);
			if (!urls.containsKey(create.getId())) {
				urls.put(create.getId(), create);
			} else {
				create = urls.get(create.getId());
			}
			create.getFiles().add(file.getName());
		}
	}
	
	private static void loadProp(final Map<String, String> map, final String fileName) throws IOException {
		Properties prop = new Properties();
		final ClassLoader classLoader = LoadConfig.class.getClassLoader();
		try (final InputStream inStream = classLoader.getResourceAsStream(fileName)) {
			prop.load(inStream);
		}
		prop.entrySet().forEach(row -> map.put(row.getKey().toString(), row.getValue().toString()));
	}

	public static String gameSubgenreDecode(final String key) {
		return getInstance().gameSubgenre.containsKey(key) ? getInstance().gameSubgenre.get(key) : key;
	}

	public static String gameGenreDecode(final String key) {
		return getInstance().gameGenre.containsKey(key) ? getInstance().gameGenre.get(key) : key;
	}

	public static String primaryClassificationDecode(final String key) {
		return getInstance().primaryClassification.containsKey(key) ? getInstance().primaryClassification.get(key) : key;
	}

	public static String secondaryClassificationDecode(final String key) {
		return getInstance().secondaryClassification.containsKey(key) ? getInstance().secondaryClassification.get(key) : key;
	}

	public static String tertiaryClassificationDecode(final String key) {
		return getInstance().tertiaryClassification.containsKey(key) ? getInstance().tertiaryClassification.get(key) : key;
	}

	private final Map<String, CoppiaUrl> preferiti = Utils.createMap();
	private final Map<String, CoppiaUrl> posseduti = Utils.createMap();
	private final MyProperties config = new MyProperties();
	private final MyProperties ricerche = new MyProperties();
	private final Set<String> idPosseduti = Utils.createSet();
	private final Map<String, String> gameSubgenre = Utils.createMap();
	private final Map<String, String> gameGenre = Utils.createMap();
	private final Map<String, String> primaryClassification = Utils.createMap();
	private final Map<String, String> secondaryClassification = Utils.createMap();
	private final Map<String, String> tertiaryClassification = Utils.createMap();

	@Data
	@RequiredArgsConstructor
	public static class CoppiaUrl {
		private final List<String> files = new ArrayList<>();
		private final String id;
		private final String originUrl;
		private final String jsonUrl;
		private final String priceJsonUrl;
		private final String addictionalJsonUrl;
		@Getter(value = AccessLevel.PRIVATE)
		private final String imageJsonUrl;
		
		public String getImageJsonUrl(Videogame videogame) {
			if (videogame != null && videogame.getConceptId() != null) {
				return imageJsonUrl.replace("{conceptId}", videogame.getConceptId());
			}
			return "";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
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
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}
	}

	@SuppressWarnings("serial")
	public static class MyProperties extends Properties {
		public Collection<String> valuesAsString() {
			final List<String> list = Utils.createList();
			for (final String key : this.stringPropertyNames()) {
				list.add(this.getProperty(key));
			}
			return list;
		}
	}
}
