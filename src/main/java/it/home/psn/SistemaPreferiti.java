package it.home.psn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import it.home.psn.module.Connection;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import it.home.psn.module.Videogame;

public class SistemaPreferiti {

	public static void main(String[] args) throws IOException {
		new SistemaPreferiti();
	}

	private SistemaPreferiti() throws IOException {
		load(config, "config.properties");
		final InputStream inStream = classLoader.getResourceAsStream("preferiti.properties");
		List<String> list = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(inStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (isOk(line)) {
					add(list,line);
				} else {
					add(list,trasforma(line));
				}
			}
		}
		System.out.println("\n\n\n\n\n\n");
		//Collections.sort(list);
		for(String str : list) {
			System.out.println(str);
		}
	}
	
	private static void add(List<String> list, String str) {
		if (!list.contains(str)) {
			list.add(str);
		}
	}

	private boolean isOk(String line) {
		if (line.isBlank()) {
			return true;
		}
		if (line.startsWith("#")) {
			return true;
		}
		return (line.contains("=") && !line.startsWith("http"));
	}

	private String trasforma(String line) throws IOException {
		final CoppiaUrl coppia = getUrls(line);
		final Videogame videogame = new Connection().getVideogame(coppia);
		return videogame != null ? videogame.getName().replaceAll("[^a-zA-Z\\d]", "_")+"="+line : "# "+line;
	}

	public CoppiaUrl getUrls(final String url) {
		final String jsonUrl = this.config.getProperty("base.json.url");
		final String targetUrl = this.config.getProperty("base.html.url");
		return new CoppiaUrl(url, url.replace(targetUrl, jsonUrl));
	}

	private void load(final Properties prop, final String fileName) throws IOException {
		final InputStream inStream = classLoader.getResourceAsStream(fileName);
		prop.load(inStream);
	}

	private final ClassLoader classLoader = getClass().getClassLoader();
	private final Properties config = new Properties();
}
