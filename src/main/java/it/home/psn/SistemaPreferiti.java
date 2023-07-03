package it.home.psn;

import static it.home.psn.Utils.createSet;
import static it.home.psn.module.Connection.getCatenaProp;
import static it.home.psn.module.LoadConfig.replace;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import it.home.psn.module.Connection;
import it.home.psn.module.LoadConfig;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import it.home.psn.module.Videogame;
import lombok.extern.log4j.Log4j;
import org.json.JSONObject;


@Log4j
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
			SistemaPreferiti s = new SistemaPreferiti(new File(root,fileName));
			s.connection.remove();
		}
	}
	

	private SistemaPreferiti(File file) throws IOException {
		load(config, "config.properties");
		List<String> list = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
			String oldLine = null;
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				line = line.replace("__", "_");
				line = line.replaceFirst("_=", "=");
				line = line.trim();

				if (oldLine == null || !oldLine.equals(line)){
					elaboraLinea(list, line);
				}
				oldLine = line;
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
		log.info(temp.getAbsolutePath());
		PrintWriter out = new PrintWriter(temp);
		
		for(String str : listDistinct) {
			out.println(str);
		}
		out.println("\n\n\n\n");
		
		out.flush();
		out.close();

		
		FileUtils.copyFile(temp, file);
		
		log.info("FINE " + file);
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
		if (isConcept(line)){
			String id = LoadConfig.extractId(line);
			var conf = LoadConfig.getInstance().getConfig();
			String sha256Hash4 = conf.getProperty("sha256Hash4");
			final String conceptUrl = replace(replace(conf.getProperty("base.json.concept.url"), "id", id), "sha256Hash4", sha256Hash4);
			JSONObject json = connection.get().createJson(conceptUrl);
			String newId = getCatenaProp(json, "data", "conceptRetrieve", "defaultProduct", "id");
			if (newId != null){
				final CoppiaUrl coppia = LoadConfig.getCoppia(newId);
				String name = getCatenaProp(json, "data", "conceptRetrieve", "defaultProduct", "name");
				return name != null ? trasformaNome(name)+"="+coppia.getOriginUrl() : "# "+line;
			}
			else{
				return "# " + line;
			}
		}
		else{
			final CoppiaUrl coppia = getUrls(line);
			final Videogame videogame = connection.get().getVideogame(coppia);
			return videogame != null ? trasformaNome(videogame)+"="+line : "# "+line;
		}
	}

	private boolean isConcept(String line){
		final String targetUrl = replace(LoadConfig.getInstance().getConfig().getProperty("base.html.concept.url"), "id", "");
		return line.contains(targetUrl);
	}

	private String trasformaNome(final Videogame videogame) {
		return trasformaNome(videogame.getName());
	}

	private String trasformaNome(final String name) {
		String str = name.replaceAll("[^a-zA-Z\\d]", "_");
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

	private final Set<String> idErrori = createSet();
	private final ThreadLocal<Connection> connection = ThreadLocal.withInitial(() -> new Connection(idErrori));
	private final ClassLoader classLoader = getClass().getClassLoader();
	private final Properties config = new Properties();
}
