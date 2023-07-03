package it.home.psn.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import it.home.psn.Constants;
import it.home.psn.SistemaPreferiti;
import it.home.psn.Utils;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Log4j
@RequiredArgsConstructor
public class Connection {

	@SneakyThrows
	public JSONObject createJson(String url){
		randomSleep();
		try {
			String json = getJson(url);
			return new JSONObject(json);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getCatenaProp(JSONObject json, String ... properties){
		JSONObject res = json;
		for (int index = 0; index < properties.length; index++) {
			String prop = properties[index];
			if (res == null){
				return null;
			}
			if (index < properties.length - 1){
				res = res.optJSONObject(prop);
			}
			else{
				return res.optString(prop);
			}
		}
		return null;
	}

	public Videogame getVideogame(final CoppiaUrl url) throws IOException{
		randomSleep();
		final String sb = getJson(url.getJsonUrl());
		final String priceSb = getJson(url.getPriceJsonUrl());
		try {
			Videogame videogame = Utils.elaboraJson(new JSONObject(sb));
			if (videogame != null) {
				videogame.setCoppia(url);
				Utils.aggiungiPrezzi(videogame, new JSONObject(priceSb));
				Utils.aggiungiPrezziExt(videogame, new JSONObject(getJson(url.getAddictionalJsonUrl())));
				String urlStr = url.getImageJsonUrl(videogame);
				if (StringUtils.isNotBlank(urlStr)) {
					Utils.aggiungiImmagini(videogame, new JSONObject(getJson(urlStr)));
				}
				idErrori.remove(url.getId());
			}
			else {
				idErrori.add(url.getId());
				log.error("Non trovo " + url.getJsonUrl());
			}
			return videogame;
		}
		catch (JSONException e) {
			idErrori.add(url.getId());
			if (Constants.DEBUG) {
				log.error("\n-----------------");
				log.error(url.getOriginUrl());
				log.error(url.getJsonUrl());
				log.error(e.getMessage() + "\n" + sb.toString()+"\n-----------------");
			}
			return null;
		}
	}

	private static String getJson(final String url) throws IOException {
		final StringBuilder sb = new StringBuilder();
		System.setProperty("java.net.useSystemProxies", "true");
		final OkHttpClient client = new OkHttpClient().newBuilder()
				.callTimeout(60, TimeUnit.SECONDS)
				.connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.build();
		
		final Request request = new Request.Builder().url(url).method("GET", null)
				.addHeader("x-psn-store-locale-override", "IT-IT")
				.build();
		final InputStream stream = client.newCall(request).execute().body().byteStream();

		final BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		return sb.toString();
	}
	
	private static void randomSleep() throws IOException {
		try {
			Thread.sleep(RandomUtils.nextLong(1, 500));
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public List<Videogame> ricerca(final String url) throws IOException{
		final StringBuilder sb = new StringBuilder();
		System.setProperty("java.net.useSystemProxies", "true");
		final OkHttpClient client = new OkHttpClient().newBuilder().build();
		final Request request = new Request.Builder().url(url).method("GET", null).build();
		final InputStream stream = client.newCall(request).execute().body().byteStream();

		final BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		// System.out.println("new Test" + sb.toString());
		try {
			return Utils.urlsRicerca(new JSONObject(sb.toString()));
		}
		catch (JSONException e) {
			if (Constants.DEBUG) {
				log.error("\n-----------------");
				log.error(url);
				log.error(e.getMessage() + "\n" + sb.toString());
				log.error("\n-----------------");
			}
			return null;
		}
	}
	
	private final Set<String> idErrori;
}
