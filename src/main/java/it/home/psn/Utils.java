package it.home.psn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import it.home.psn.module.Videogame;
import it.home.psn.module.Videogame.Genere;
import it.home.psn.module.Videogame.Preview;
import it.home.psn.module.Videogame.Sconto;
import it.home.psn.module.Videogame.Screenshot;
import it.home.psn.module.Videogame.Tipo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {
	public static <T> Set<T> createSet() {
		return ConcurrentHashMap.newKeySet();
	}

	public static <K,V> Map<K,V> createMap() {
		return new ConcurrentHashMap<>();
	}

	public static <T> List<T> createList() {
		return new CopyOnWriteArrayList<>();
	}
	
	public static <K> void add(final Map<K,Integer> mappa, final K key) {
		if (!mappa.containsKey(key)) {
			mappa.put(key, 1);
		}
		else {
			final int counter = mappa.get(key);
			mappa.put(key, counter + 1);
		}
	}
	
	public static <T extends Comparable<T>> int compare(T obj1, T obj2) {
		if (Objects.equals(obj1, obj2)) {
			return 0;
		}
		if (obj1 == null) {
			return -1;
		}
		if (obj2 == null) {
			return +1;
		}
		return obj1.compareTo(obj2);
	}
	
	public static List<Videogame> urlsRicerca(final JSONObject response) {
		final List<Videogame> list = Utils.createList();
		final JSONArray links = response.optJSONArray("links");
		if (links != null) {
			for (int index = 0; index < links.length(); index++) {
				final JSONObject obj = links.getJSONObject(index);
				final Videogame videogame = elaboraJson(obj);
				if (videogame != null) {
					list.add(videogame);
				}
			}
		}
		return list;
	}

	public static Videogame elaboraJson(final JSONObject response) {
		//default_sku->entitlements[id==id_link,name,packages[platformName,size],subtitle_language_codes[],voice_language_codes[],metadata->{voiceLanguageCode[],subtitleLanguageCode[]} ]
		//mediaList->previews[type,typeId,source,url,order,streamUrl,shots[]]

		final String id = response.optString("id");
		final String name = response.optString("name");
		if (StringUtils.isEmpty(id) || StringUtils.isEmpty(name)) {
			if (Constants.DEBUG) {
				System.err.println(response);
			}
			return null;
		}
		final Videogame videogame = new Videogame(id);
		videogame.setName(name);

		setGenere(response, videogame);
		setTipo(response, videogame);
		setDefaultSku(response, videogame);
		setLinks(response, videogame);

		final JSONObject mediaList = response.optJSONObject("mediaList");
		if (mediaList != null) {
			final JSONArray screenshots = mediaList.optJSONArray("screenshots");
			if (screenshots != null) {
				for (int index = 0; index < screenshots.length(); index++) {
					final JSONObject obj = screenshots.getJSONObject(index);
					final Screenshot screenshot = new Screenshot();
					screenshot.setOrder(obj.optInt("order"));
					screenshot.setSource(obj.optString("source"));
					screenshot.setType(obj.optString("type"));
					screenshot.setTypeId(obj.optString("typeId"));
					screenshot.setUrl(obj.optString("url"));
					videogame.getScreenshots().add(screenshot);
				}
			}
			final JSONArray previews = mediaList.optJSONArray("previews");
			if (previews != null) {
				for (int index = 0; index < previews.length(); index++) {
					final JSONObject obj = previews.getJSONObject(index);
					final Preview preview = new Preview();
					preview.setOrder(obj.optInt("order"));
					preview.setSource(obj.optString("source"));
					preview.setStreamUrl(obj.optString("streamUrl"));
					preview.setType(obj.optString("type"));
					preview.setTypeId(obj.optString("typeId"));
					preview.setUrl(obj.optString("url"));

					final JSONArray shots = obj.optJSONArray("shots");
					if (shots != null) {
						for (int j = 0; j < shots.length(); j++) {
							preview.getShots().add(shots.getString(j));
						}
					}
					videogame.getPreviews().add(preview);
				}
			}
		}

		// System.out.println("Get response json from " + this.serverUrl + "\n" +
		// sb.toString().length());
		return videogame;
	}

	private static void setLinks(final JSONObject response, final Videogame videogame) {
		final JSONArray links = response.optJSONArray("links");
		if (links != null) {
			for (int index = 0; index < links.length(); index++) {
				final JSONObject obj = links.getJSONObject(index);
				final String relatedId = obj.optString("id");
				if (!StringUtils.isEmpty(relatedId)) {
					videogame.getOtherIds().add(relatedId);
				} else {
					if (Constants.DEBUG) {
						System.err.println(obj);
					}
				}
			}
		}
	}

	private static void setDefaultSku(final JSONObject response, final Videogame videogame) {
		final JSONObject defaultSku = response.optJSONObject("default_sku");
		if (defaultSku != null) {
			videogame.setDisplayPrizeFull(defaultSku.optString("display_price"));
			videogame.setPriceFull(convertPrice(defaultSku.optInt("price")));
			final JSONArray rewards = defaultSku.optJSONArray("rewards");
			if (rewards != null) {
				for (int index = 0; index < rewards.length(); index++) {
					final Sconto sconto = new Sconto();
					final JSONObject obj = rewards.getJSONObject(index);
					sconto.setDiscount(obj.optInt("discount"));
					sconto.setPrice(convertPrice(obj.optInt("price")));
					sconto.setDisplayPrice(obj.optString("display_price"));
					// final String start = obj.optString("start_date");
					// final String end = obj.optString("end_date");
					sconto.setPlus(obj.optBoolean("isPlus"));
					sconto.setEAAccess(obj.optBoolean("isEAAccess"));
					videogame.getSconti().add(sconto);
				}
			}
		}
	}

	private static void setTipo(final JSONObject response, final Videogame videogame) {
		final JSONArray gameContentTypesList = response.optJSONArray("gameContentTypesList");
		if (gameContentTypesList != null) {
			for (int index = 0; index < gameContentTypesList.length(); index++) {
				final JSONObject obj = gameContentTypesList.getJSONObject(index);
				final Tipo tipo = new Tipo();
				tipo.setName(obj.optString("name"));
				tipo.setKey(obj.optString("key"));
				videogame.getTipi().add(tipo);
			}
		}
		if (gameContentTypesList == null || gameContentTypesList.length() == 0) {
			final Tipo tipo = new Tipo();
			tipo.setName(UNKNOWN);
			tipo.setKey(UNKNOWN);
			videogame.getTipi().add(tipo);
		}
	}

	private static void setGenere(final JSONObject response, final Videogame videogame) {
		final JSONObject attributes = response.optJSONObject("attributes");
		if (attributes != null) {
			final JSONObject facets = attributes.optJSONObject("facets");
			if (facets != null) {
				final JSONArray genre = facets.optJSONArray("genre");
				if (genre != null) {
					for (int index = 0; index < genre.length(); index++) {
						final JSONObject obj = genre.getJSONObject(index);
						final Genere genere = new Genere();
						genere.setName(obj.optString("name"));
						genere.setCount(obj.optInt("count"));
						genere.setKey(obj.optString("key"));
						videogame.getGeneri().add(genere);
					}
				}
				if (genre == null || genre.length() == 0) {
					final Genere genere = new Genere();
					genere.setName(UNKNOWN);
					genere.setCount(1);
					genere.setKey(UNKNOWN);
					videogame.getGeneri().add(genere);
				}
			}
		}
	}

	private static BigDecimal convertPrice(final int price) {
		return new BigDecimal(price).divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);
	}

	private static final String UNKNOWN = "_Sconosciuto";
}
