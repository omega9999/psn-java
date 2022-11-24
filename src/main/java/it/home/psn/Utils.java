package it.home.psn;
import static it.home.psn.module.Videogame.Problema.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import it.home.psn.module.LoadConfig;
import it.home.psn.module.Videogame;
import it.home.psn.module.Videogame.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

@Log4j
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

	public static <K> void add(final Map<K,Map<K,Integer>> mappa, final K keyExternal, final K keyInternal) {
		if (!mappa.containsKey(keyExternal)) {
			mappa.put(keyExternal, createMap());
		}
		add(mappa.get(keyExternal), keyInternal);
	}
	
	public static <T extends Comparable<T>> int compare(T obj1, T obj2) {
		if (Objects.equals(obj1, obj2)) {
			return 0;
		}
		if (obj1 == null) {
			return +1;
		}
		if (obj2 == null) {
			return -1;
		}
		return obj1.compareTo(obj2);
	}
	
	public static List<Videogame> urlsRicerca(final JSONObject response) {
		final List<Videogame> list = Utils.createList();
		final JSONArray links = response.optJSONArray("links");
		if (links != null) {
			for (int index = 0; index < links.length(); index++) {
				final JSONObject obj = links.optJSONObject(index);
				final Videogame videogame = elaboraJson(obj);
				if (videogame != null) {
					list.add(videogame);
				}
			}
		}
		return list;
	}
	
	private static  List<String> getMetadata(final JSONObject response) {
		final List<String> sb = createList();
		if (response != null) {
			final JSONArray values = response.optJSONArray("values");
			if (values != null) {
				for (int index = 0; index < values.length(); index++) {
					sb.add(values.getString(index));
				}
			}
		}
		return sb;
	}
	
	private static final List<String> PRIORITA = Arrays.asList("12", "10", "13", "14", "1", "9", "2");
	
	public static Videogame aggiungiPrezzi(Videogame videogame, final JSONObject response) {
		JSONObject data = response.optJSONObject("data");
		if (data != null) {
			JSONObject productRetrieve = data.optJSONObject("productRetrieve");
			if (productRetrieve != null) {
				JSONObject concept = productRetrieve.optJSONObject("concept");
				if (concept != null) {
					videogame.setConceptId(concept.optString("id"));
				}
				if (videogame.getConceptId() == null) {
					videogame.setBitmaskProblemi(CONCEPT_ID_NULLO);
				}
				JSONArray webctas = productRetrieve.optJSONArray("webctas");
				for (int index = 0; index < webctas.length(); index++) {
					final JSONObject webcta = webctas.optJSONObject(index);
					JSONObject price = webcta.optJSONObject("price");
					String type = webcta.optString("type");
					if (List.of("DOWNLOAD_TRIAL", "UPSELL_PS_PLUS_GAME_CATALOG", "UPSELL_PS_PLUS_FREE",
							"UPSELL_PS_PLUS_CLASSIC_GAME_COLLECTION", "UPSELL_EA_ACCESS_FREE").contains(type)) {
						// non considero questi sconti
						continue;
					}
					String discountedValue = price.optString("discountedValue", null);
					String basePriceValue = price.optString("basePriceValue", null);
					String currencyCode = price.optString("currencyCode");
					if (discountedValue != null && !discountedValue.equals(basePriceValue)) {
						final Sconto sconto = new Sconto();
						if (!StringUtils.equals("EUR", currencyCode)) {
							videogame.setBitmaskProblemi(SCONTO_NON_EUR);
							double ratio = Double.valueOf(discountedValue) / Double.valueOf(basePriceValue);
							if (String.valueOf(ratio).equalsIgnoreCase("NaN")) {
								videogame.setBitmaskProblemi(RATIO_SCONTO_NAN);
								sconto.setPrice(BigDecimal.valueOf(-1));
							}
							else {
								sconto.setPrice(videogame.getPriceFull().multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.ONE, 2, RoundingMode.HALF_DOWN));
							}
						}
						else {
							sconto.setPrice(convertPrice(Integer.valueOf(discountedValue)));
						}
						//sconto.setDiscount(null);
						sconto.setDisplayPrice(price.optString("discountedPrice"));
						//sconto.setPlus(obj.optBoolean("isPlus"));
						//sconto.setEAAccess(obj.optBoolean("isEAAccess"));
						videogame.getSconti().add(sconto);
					}
				}
			}
			else {
				videogame.setBitmaskProblemi(PRODUCT_RETRIEVE_NULLO);
			}
		}
		return videogame;
	}
	
	public static void aggiungiImmagini(Videogame videogame, JSONObject response) {
		if (response == null || response.optJSONObject("errors") != null) {
			videogame.setBitmaskProblemi(IMMAGINI_AGGIUNTIVE_ERRORE);
		}
		JSONObject data = response.optJSONObject("data");
		if (data != null) {
			JSONObject conceptRetrieve = data.optJSONObject("conceptRetrieve");
			if (conceptRetrieve != null) {
				
				setMedias(videogame, "media", conceptRetrieve.optJSONArray("media"));
				JSONArray products = conceptRetrieve.optJSONArray("products");
				if (products != null) {
					for (int index = 0; index < products.length(); index++) {
						final JSONObject product = products.optJSONObject(index);
						setMedias(videogame, "product-media",  product.optJSONArray("media"));
					}
				}
			}
		}
		
		
	}

	private static void setMedias(Videogame videogame, final String prefix, JSONArray medias) {
		if (medias != null) {
			for (int index = 0; index < medias.length(); index++) {
				final JSONObject media = medias.optJSONObject(index);
				setMedia(videogame, prefix, media);
			}
		}
	}

	private static void setMedia(Videogame videogame, final String prefix, final JSONObject media) {
		String role = media.optString("role");
		String type = media.optString("type");
		String url = media.optString("url");
		
		switch (type) {
		case "VIDEO":
			Video video = new Video();
			video.setType(role);
			video.setUrl(url);
			videogame.getVideos().add(video);
			break;

		default:
			if ("SCREENSHOT".equalsIgnoreCase(role)) {
				Screenshot image = new Screenshot();
				image.setType(type + "-" + role);
				image.setUrl(url);
				//image.setTypeData(TypeData.IMAGE);
				//image.setSubTypeData(String.valueOf(type));
				videogame.getScreenshots().add(image);
			}
			else {
				Preview image = new Preview();
				image.setType(type + "-" + role);
				image.setUrl(url);
				//image.setTypeData(TypeData.IMAGE);
				//image.setSubTypeData(String.valueOf(type));
				videogame.getPreviews().add(image);
			}
			break;
		}
	}
	
	public static void aggiungiPrezziExt(Videogame videogame, JSONObject response) {
		if (response == null || response.optJSONObject("errors") != null) {
			videogame.setBitmaskProblemi(PREZZO_AGGIUNTIVO_ERRORE);
		}
		// TODO Auto-generated method stub ???
		
	}

	public static Videogame elaboraJson(final JSONObject response) {
		final String id = response.optString("id");
		final String name = response.optString("name");
		if (StringUtils.isEmpty(id) || StringUtils.isEmpty(name)) {
			if (Constants.DEBUG) {
				log.error(response);
			}
			return null;
		}
		final Videogame videogame = new Videogame(id);
		videogame.setName(name);
		videogame.setJson(response.toString());
		videogame.setDescription(response.optString("long_desc"));
		
		videogame.setPosseduto(LoadConfig.getInstance().getIdPosseduti().contains(id));
		
		setDefaultSku(response.optJSONObject("default_sku"), videogame);
		setSkus(response.optJSONArray("skus"), videogame);
		setGenere(response, videogame);
		setTipo(response, videogame);
		setLinks(response, videogame);
		setParentLinks(response, videogame);
		
		final JSONObject metadata = response.optJSONObject("metadata");
		elaboraMetadata(videogame, metadata);
		
		final JSONArray images = response.optJSONArray("images");
		if (images != null) {
			final List<Screenshot> list = createList();
			for (int index = 0; index < images.length(); index++) {
				final JSONObject obj = images.optJSONObject(index);
				final Integer type = obj.optInt("type");
				final String url = obj.optString("url");
				final Screenshot image = new Screenshot();
				image.setTypeId(String.valueOf(type));
				image.setType("image");
				image.setTypeData(TypeData.IMAGE);
				image.setSubTypeData(String.valueOf(type));
				image.setUrl(url);
				if (url != null) {
					list.add(image);
				}
			}
			if (!list.isEmpty()) {
				Collections.sort(list, (a,b)->{
					int indexA = PRIORITA.indexOf(a.getSubTypeData());
					int indexB = PRIORITA.indexOf(b.getSubTypeData());
					if (indexA < 0) {
						indexA = Integer.MAX_VALUE;
					}
					if (indexB < 0) {
						indexB = Integer.MAX_VALUE;
					}
					return Integer.compare(indexA, indexB);
				});
				//videogame.getScreenshots().addAll(list);
				videogame.getScreenshots().add(list.get(0));
			}
			
		}
		
		final JSONArray promomedia = response.optJSONArray("promomedia");
		if (promomedia != null) {
			for (int index = 0; index < promomedia.length(); index++) {
				final JSONObject obj = promomedia.optJSONObject(index);
				final Video video = new Video();
				video.setTypeData(TypeData.PROMEDIA);
				video.setUrl(obj.optString("url"));
				if (!StringUtils.isBlank(video.getUrl())) {
					videogame.getVideos().add(video);
				}
			}
		}
		

		final JSONObject mediaList = response.optJSONObject("mediaList");
		if (mediaList != null) {
			final JSONArray screenshots = mediaList.optJSONArray("screenshots");
			if (screenshots != null) {
				for (int index = 0; index < screenshots.length(); index++) {
					final JSONObject obj = screenshots.optJSONObject(index);
					final Screenshot screenshot = new Screenshot();
					screenshot.setOrder(obj.optInt("order"));
					screenshot.setSource(obj.optString("source"));
					screenshot.setType(obj.optString("type"));
					screenshot.setTypeId(obj.optString("typeId"));
					screenshot.setUrl(obj.optString("url"));
					screenshot.setTypeData(TypeData.SCREENSHOT);
					screenshot.setSubTypeData(screenshot.getTypeId());
					videogame.getScreenshots().add(screenshot);
				}
			}
			final JSONArray previews = mediaList.optJSONArray("previews");
			if (previews != null) {
				for (int index = 0; index < previews.length(); index++) {
					final JSONObject obj = previews.optJSONObject(index);
					final Preview preview = new Preview();
					preview.setOrder(obj.optInt("order"));
					preview.setSource(obj.optString("source"));
					preview.setStreamUrl(obj.optString("streamUrl"));
					preview.setType(obj.optString("type"));
					preview.setTypeId(obj.optString("typeId"));
					preview.setUrl(obj.optString("url"));
					preview.setTypeData(TypeData.PREVIEW);
					preview.setSubTypeData(preview.getTypeId());
					
					final Video video = new Video();
					video.setTypeData(TypeData.PREVIEW);
					video.setUrl(obj.optString("url"));
					if (!StringUtils.isBlank(video.getUrl())) {
						videogame.getVideos().add(video);
					}

					final JSONArray shots = obj.optJSONArray("shots");
					if (shots != null) {
						for (int j = 0; j < shots.length(); j++) {
							final Screenshot screenshot = new Screenshot();
							screenshot.setUrl(shots.getString(j));
							screenshot.setTypeData(TypeData.SHOT);
							screenshot.setSubTypeData(String.valueOf(j));
							preview.getShots().add(screenshot);
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

	private static void elaboraMetadata(final Videogame videogame, final JSONObject metadata) {
		if (metadata != null) {
			for (final String tag : metadata.keySet()) {
				final List<String> list = getMetadata(metadata.optJSONObject(tag));
				switch (tag) {
				case "genre":
					for (String str : list) {
						final Genere genere = new Genere();
						genere.setName(str);
						videogame.getGeneri().add(genere);
					}
					break;
				case "game_genre":
					for (String str : list) {
						final Genere genere = new Genere();
						genere.setName(LoadConfig.gameGenreDecode(str));
						genere.setKey(str);
						videogame.getGeneri().add(genere);
					}
					break;
				case "subgenre":
				case "game_subgenre":
					for (String str : list) {
						final Genere genere = new Genere();
						genere.setName(LoadConfig.gameSubgenreDecode(str));
						genere.setKey(str);
						videogame.getSubgeneri().add(genere);
					}
					break;
				case "primary_classification":
					for (String str : list) {
						final Tipo tipo = new Tipo();
						tipo.setName(LoadConfig.primaryClassificationDecode(str));
						tipo.setKey(str);
						videogame.getTipi().add(tipo);
					}
					break;
				case "secondary_classification":
					for (String str : list) {
						final Tipo tipo = new Tipo();
						tipo.setName(LoadConfig.secondaryClassificationDecode(str));
						tipo.setKey(str);
						videogame.getTipi().add(tipo);
					}
					break;
				case "tertiary_classification":
					for (String str : list) {
						final Tipo tipo = new Tipo();
						tipo.setName(LoadConfig.tertiaryClassificationDecode(str));
						tipo.setKey(str);
						videogame.getTipi().add(tipo);
					}
					break;
				case "cn_vrEnabled":
					videogame.setEnableVr(elaboraBooleanMetadata(list));
					break;
					
				case "cn_vrRequired":
					videogame.setRequiredVr(elaboraBooleanMetadata(list));
					break;

				case "cn_onlinePlayMode":
					videogame.setOnline(elaboraBooleanMetadata(list));
					break;

				case "playable_platform":
					videogame.getPlatform().addAll(list);
					break;
				default:
					videogame.getAllMetadataValues().putIfAbsent("UNKNOWN-TAG", Utils.createList());
					List<String> tmp = videogame.getAllMetadataValues().get("UNKNOWN-TAG");
					tmp.add(tag);
					videogame.getAllMetadataValues().put("UNKNOWN-TAG", tmp);
					break;
				}
				videogame.getAllMetadata().add(tag);
				videogame.getAllMetadataValues().put(tag, list);
			}
		}
	}

	private static Flag elaboraBooleanMetadata(final List<String> list) {
		Flag flag = null;
		for (String str : list) {
			flag = Flag.valueOf(str);
			break;
		}
		return flag;
	}

	private static void setSkus(final JSONArray skus, final Videogame videogame) {
		if (skus != null) {
			for (int index = 0; index < skus.length(); index++) {
				final JSONObject obj = skus.optJSONObject(index);
				setDefaultSku(obj.optJSONObject("default_sku"), videogame);
			}
		}
	}
	
	private static void setParentLinks(final JSONObject response, final Videogame videogame) {
		final JSONArray links = response.optJSONArray("parent_links");
		if (links != null) {
			for (int index = 0; index < links.length(); index++) {
				final JSONObject obj = links.optJSONObject(index);
				final String relatedId = obj.optString("id");
				if (!StringUtils.isEmpty(relatedId)) {
					videogame.getOtherIds().add(relatedId);
					videogame.getParentIds().add(relatedId);
					videogame.getParentUrls().add(LoadConfig.getCoppia(relatedId).getOriginUrl());
				} else {
					if (Constants.DEBUG) {
						log.error(obj);
					}
				}
			}
		}
	}

	private static void setLinks(final JSONObject response, final Videogame videogame) {
		final JSONArray links = response.optJSONArray("links");
		if (links != null) {
			for (int index = 0; index < links.length(); index++) {
				final JSONObject obj = links.optJSONObject(index);
				final String relatedId = obj.optString("id");
				if (!StringUtils.isEmpty(relatedId)) {
					videogame.getOtherIds().add(relatedId);
				} else {
					if (Constants.DEBUG) {
						log.error(obj);
					}
				}
			}
		}
	}

	private static void setDefaultSku(final JSONObject defaultSku, final Videogame videogame) {
		if (defaultSku != null) {
			videogame.setDisplayPrizeFull(defaultSku.optString("display_price"));
			videogame.setPriceFull(convertPrice(defaultSku.optInt("price")));
			setRewards(defaultSku.optJSONArray("rewards"), videogame);
			setEntitlements(defaultSku.optJSONArray("entitlements"), videogame);
		}
	}

	private static void setEntitlements(final JSONArray entitlements, final Videogame videogame) {
		if (entitlements != null) {
			for (int index = 0; index < entitlements.length(); index++) {
				final JSONObject obj = entitlements.optJSONObject(index);
				videogame.getVoices().addAll(jsonArray2ListString(obj.optJSONArray("voice_language_codes")));
				videogame.getSubtitles().addAll(jsonArray2ListString(obj.optJSONArray("subtitle_language_codes")));
				final JSONObject metadata = obj.optJSONObject("metadata");
				if (metadata != null) {
					videogame.getVoices().addAll(jsonArray2ListString(metadata.optJSONArray("voiceLanguageCode")));
					videogame.getSubtitles().addAll(jsonArray2ListString(metadata.optJSONArray("subtitleLanguageCode")));
				}
			}
		}
	}
	
	private static List<String> jsonArray2ListString(final JSONArray array){
		final List<String> list = createList();
		if (array != null) {
			for (int index = 0; index < array.length(); index++) {
				final String str = array.getString(index);
				if (str != null) {
					list.add(str);
				}
			}
		}
		return list;
	}

	private static void setRewards(final JSONArray rewards, final Videogame videogame) {
		if (rewards != null) {
			for (int index = 0; index < rewards.length(); index++) {
				final Sconto sconto = new Sconto();
				final JSONObject obj = rewards.optJSONObject(index);
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

	private static void setTipo(final JSONObject response, final Videogame videogame) {
		final JSONArray gameContentTypesList = response.optJSONArray("gameContentTypesList");
		if (gameContentTypesList != null) {
			for (int index = 0; index < gameContentTypesList.length(); index++) {
				final JSONObject obj = gameContentTypesList.optJSONObject(index);
				final Tipo tipo = new Tipo();
				tipo.setName(obj.optString("name"));
				tipo.setKey(obj.optString("key"));
				videogame.getTipi().add(tipo);
			}
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
						final JSONObject obj = genre.optJSONObject(index);
						final Genere genere = new Genere();
						genere.setName(obj.optString("name"));
						genere.setCount(obj.optInt("count"));
						genere.setKey(obj.optString("key"));
						videogame.getGeneri().add(genere);
					}
				}
			}
		}
	}

	private static BigDecimal convertPrice(final int price) {
		return new BigDecimal(price).divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);
	}


}
