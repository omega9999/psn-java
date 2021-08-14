package it.home.psn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import it.home.psn.module.Connection;
import it.home.psn.module.LoadConfig;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import it.home.psn.module.Videogame;

public class SistemaPreferiti {

	public static final File root = new File("./preferiti/");
	private static final String[] FILES = {
			"preferiti.properties",
			"posseduti-digitale.properties",
			"posseduti-demo.properties",
			"posseduti-fisico.properties"
	};
	
	
	public static void main(String[] args) throws IOException {
		for(String fileName : FILES) {
			new SistemaPreferiti(new File(root,fileName));
		}
	}
	

	private SistemaPreferiti(File file) throws IOException {
		load(config, "config.properties");
		List<String> list = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				line = line.replace("__", "_");
				line = line.replaceFirst("_=", "=");
				elaboraLinea(list, line);
			}
		}
		
		List<String> listDistinct = list.stream().distinct().collect(Collectors.toList());
		
		Collections.sort(listDistinct, (a,b)->{
			int tmp = 0;
			
			tmp = priorizza(a, b, (x->x.startsWith("#")));
			if (tmp != 0) {
				return tmp;
			}
			tmp = priorizza(a, b, (x->x.toLowerCase().contains("_demo=")));
			if (tmp != 0) {
				return tmp;
			}
			tmp = priorizza(a, b, (x->x.toLowerCase().contains("_demo_")));
			if (tmp != 0) {
				return tmp;
			}

			return a.toLowerCase().compareTo(b.toLowerCase());
		});
		
		File temp = File.createTempFile(file.getName(), ".properties");
		System.err.println(temp.getAbsolutePath());
		PrintWriter out = new PrintWriter(temp);
		
		for(String str : listDistinct) {
			out.println(str);
		}
		out.println("\n\n\n\n");
		
		out.flush();
		out.close();

		
		FileUtils.copyFile(temp, file);
		
		System.err.println("FINE " + file);
	}


	private void elaboraLinea(List<String> list, String line) throws IOException {
		if (isOk(line)) {
			if (!line.startsWith("#")) {
				add(list,line);
			}
			else {
				String line1 = line.substring(1).trim();
				elaboraLinea(list, line1);
			}
		} else {
			add(list,trasforma(line));
		}
	}
	
	private static int priorizza(String a, String b, Function<String, Boolean> function) {
		if (function.apply(a) && !function.apply(b)) {
			return -1;
		}
		if (!function.apply(a) && function.apply(b)) {
			return +1;
		}
		return 0;
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
		return videogame != null ? trasformaNome(videogame)+"="+line : "# "+line;
	}

	private String trasformaNome(final Videogame videogame) {
		String str = videogame.getName().replaceAll("[^a-zA-Z\\d]", "_");
		String old = "";
		while(!str.equals(old)) {
			old = str;
			str = str.replace("__", "_");
			if (str.startsWith("_")) {
				str = str.substring(1);
			}
		}
		return str;
	}

	public CoppiaUrl getUrls(final String url) {
		return LoadConfig.create(url);
	}

	private void load(final Properties prop, final String fileName) throws IOException {
		final InputStream inStream = classLoader.getResourceAsStream(fileName);
		prop.load(inStream);
	}

	private final ClassLoader classLoader = getClass().getClassLoader();
	private final Properties config = new Properties();
}
