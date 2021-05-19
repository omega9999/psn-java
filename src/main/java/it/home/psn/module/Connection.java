package it.home.psn.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.json.JSONException;
import org.json.JSONObject;

import it.home.psn.Constants;
import it.home.psn.Utils;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Connection {

	public Videogame getVideogame(final CoppiaUrl url) throws IOException{
		randomSleep();
		final StringBuilder sb = new StringBuilder();
		System.setProperty("java.net.useSystemProxies", "true");
		final OkHttpClient client = new OkHttpClient().newBuilder()
				.callTimeout(60, TimeUnit.SECONDS)
				.connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.build();
		
		final Request request = new Request.Builder().url(url.getJsonUrl()).method("GET", null).build();
		final InputStream stream = client.newCall(request).execute().body().byteStream();

		final BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		// System.out.println("new Test" + sb.toString());
		try {
			Videogame videogame = Utils.elaboraJson(new JSONObject(sb.toString()));
			if (videogame != null) {
				videogame.setCoppia(url);
			}
			return videogame;
		}
		catch (JSONException e) {
			if (Constants.DEBUG) {
				System.err.println("\n-----------------");
				System.err.println(url.getOriginUrl());
				System.err.println(url.getJsonUrl());
				System.err.println(e.getMessage() + "\n" + sb.toString()+"\n-----------------");
			}
			return null;
		}
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
				System.err.println("\n-----------------");
				System.err.println(url);
				System.err.println(e.getMessage() + "\n" + sb.toString());
				System.err.println("\n-----------------");
			}
			return null;
		}
	}
}
