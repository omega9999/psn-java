package it.home.psn.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
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
			load(preferiti, "posseduti-demo.properties");
			
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
	
	public static String subGenDecode(final String key) {
		final Map<String, String> decode = Utils.createMap();
		decode.put("RUN_AND_GUN", "Corri e Tira");
		decode.put("STEALTH", "Action stealth");
		decode.put("VEHICULAR_COMBAT", "Combattimento veicoli");
		decode.put("MYSTERY", "Mistero");
		decode.put("GRAPHIC_ADVENTURE", "Avventura grafica");
		decode.put("PHYSICS_GAME", "Gioco di fisica");
		decode.put("TURN_BASED_STRATEGY", "Strategia a turni");
		decode.put("TACTICAL", "Tattico");
		decode.put("2D_FIGHTING", "Combattimento 2D");
		decode.put("THIRD_PERSON_SHOOTER", "Sparatutto in 3^ pers.");
		decode.put("PLATFORMER", "Piattaforme");
		decode.put("DUNGEON_CRAWLER", "GDR dungeon crawler");
		decode.put("DANCE", "Danza");
		decode.put("REAL_TIME_STRATEGY", "Strategia in tempo reale");
		decode.put("SHOOT_EM_UP", "Sparatutto");
		decode.put("STRATEGY_ROLE_PLAYING_GAME", "GDR Strategico");
		decode.put("ART/EXPERIMENTAL", "Artistico sperimentale");
		decode.put("3D_FIGHTING", "Combattimento 3D");
		decode.put("BOARD_GAME", "Gioco da tavolo");
		decode.put("FANTASY", "Fantasy");
		decode.put("TEAM_FIGHTING", "Combattimento a squadre");
		decode.put("PINBALL", "Flipper");
		decode.put("MASSIVELY_MULTIPLAYER_ONLINE_ROLE_PLAYING_GAME", "MMORPG");
		decode.put("COMBAT", "Combattimento");
		decode.put("EPIC", "Epico");
		decode.put("TRIVIA/QUIZ", "Quiz");
		decode.put("FLIGHT_COMBAT", "Combattimento aereo");
		decode.put("HACK_AND_SLASH", "Hack and slash");
		decode.put("BEAT_EM_UP", "Beat 'em up");
		decode.put("TEXT_ADVENTURE", "Avventura testuale");
		decode.put("FLIGHT_SIMULATION", "Simulatore di volo");
		decode.put("DEVELOPMENT", "Sviluppo");
		decode.put("CARD_GAME", "Gioco di carte");
		decode.put("TOWER_DEFENSE", "Difesa della torre");
		decode.put("FIRST_PERSON_SHOOTER", "Sparatutto in 1^ pers.");
		decode.put("CHILDREN'S", "Per bambini");
		return decode.containsKey(key) ? decode.get(key) : key;
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
