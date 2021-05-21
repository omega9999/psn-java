package it.home.psn;

import static it.home.psn.Utils.add;
import static it.home.psn.Utils.createList;
import static it.home.psn.Utils.createMap;
import static it.home.psn.Utils.createSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import it.home.psn.Constants.Test;
import it.home.psn.module.Connection;
import it.home.psn.module.HtmlTemplate;
import it.home.psn.module.LoadConfig;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import it.home.psn.module.Videogame;
import it.home.psn.module.Videogame.AbstractUrl;
import it.home.psn.module.Videogame.Genere;
import it.home.psn.module.Videogame.Preview;
import it.home.psn.module.Videogame.Screenshot;
import it.home.psn.module.Videogame.SottoSoglia;
import it.home.psn.module.Videogame.Tipo;

public class Psn {
	public static void main(String[] args) throws Exception {
		System.err.println("Inizio");
		final Psn psn = new Psn();
		psn.preferitiInSconto();
		// psn.elenco();
		System.clearProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
		System.err.println("Fine");
	}

	public Psn() {
		this.config = LoadConfig.getInstance();
	}

	private synchronized void addTh() {
		final long id = Thread.currentThread().getId();
		if (!this.threads.containsKey(id)) {
			this.threads.put(id, 0);
		}
		this.threads.put(id, this.threads.get(id) + 1);
	}

	private void preferitiInSconto() throws Exception {
		System.clearProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
		// System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
		// String.valueOf(3 - 1));

		Writer output = new Writer();

		this.config.checkPreferiti();

		final Set<Videogame> videogames = createSet();
		final long startTime = new Date().getTime();

		if (Constants.TEST == Test.NO) {
			this.config.getUrls().parallelStream().forEach(coppia -> {
				addTh();
				try {
					final Videogame videogame = new Connection().getVideogame(coppia);
					if (videogame != null) {
						videogames.add(videogame);
					}
				} catch (IOException e) {
					System.err.println("\n-----------------");
					System.err.println("Problemi di connessione " + e.getMessage() + " " + e.getClass());
					System.err.println(coppia.getOriginUrl());
					System.err.println(coppia.getJsonUrl());
					System.err.println("\n-----------------");
				}
			});
		} else {
			videogames.addAll(output.read());
		}

		final List<Videogame> separatore = Utils.createList();
		final Videogame fake = new Videogame("");
		fake.setCoppia(new CoppiaUrl("",""));
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);

		final long midTime = new Date().getTime();
		final int trovatiMid = videogames.size();

		if (Constants.TEST == Test.NO) {
			output.writeClose(videogames);
			cercaCollegati(videogames);
			output.writeCloseEsteso(videogames);
		}

		final long endTime = new Date().getTime();

		final int trovati = videogames.size();

		final List<Videogame> videogameSorted = Arrays.asList(videogames.toArray(new Videogame[0]));
		Collections.sort(videogameSorted, (a, b) -> {
			if (a.showScreenshot(Constants.TIPO_TOP) && !b.showScreenshot(Constants.TIPO_TOP)) {
				return -1;
			}
			if (!a.showScreenshot(Constants.TIPO_TOP) && b.showScreenshot(Constants.TIPO_TOP)) {
				return +1;
			}

			if (a.isPosseduto() && !b.isPosseduto()) {
				return +1;
			}
			if (!a.isPosseduto() && b.isPosseduto()) {
				return -1;
			}

			return a.compareTo(b);
		});

		final Map<String, Integer> tipo = createMap();
		final Map<String, Integer> genere = createMap();
		final Map<String, Integer> unKnownMetadata = createMap();
		final Map<String, Integer> subgenere = createMap();
		final Map<String, Integer> screenshot = createMap();
		final Map<String, Integer> preview = createMap();
		final Map<String, Integer> genericDataType = createMap();
		final Map<String, Integer> genericDataSubType = createMap();
		final Map<String, String> genericDataSubTypeUrl = createMap();
		for (Videogame videogame : videogameSorted) {
			for (Genere g : videogame.getGeneri()) {
				add(genere, g.getName());
			}
			for (Genere g : videogame.getSubgeneri()) {
				add(subgenere, g.getName());
			}
			for (Tipo t : videogame.getTipi()) {
				add(tipo, t.getName());
			}
			for (Screenshot t : videogame.getScreenshots()) {
				add(screenshot, t.getType());
			}
			for (Preview t : videogame.getPreviews()) {
				add(preview, t.getType());
			}
			for (String t : videogame.getUnKnownMetadata()) {
				add(unKnownMetadata, t);
			}
			
			final Set<AbstractUrl> data = createSet();
			CollectionUtils.addAll(data, videogame.getScreenshots());
			CollectionUtils.addAll(data, videogame.getVideos());
			CollectionUtils.addAll(data, videogame.getPreviews());
			for (AbstractUrl t : data) {
				final String[] str = t.getUrl().split("\\.");
				add(genericDataType, str[str.length - 1].toLowerCase());
				add(genericDataSubType, t.getTypeData()+"_"+t.getSubTypeData());
				genericDataSubTypeUrl.put(t.getTypeData()+"_"+t.getSubTypeData(), t.getUrl());
			}
		}
		final List<Videogame> toHtml = Utils.createList();
		final List<Videogame> toHtmlPosseduti = Utils.createList();
		final List<Videogame> toHtmlPreferiti = Utils.createList();
		for (Videogame videogame : videogameSorted) {
			if (videogame.getJson().contains(".mp4")) {
				// output.mp4(videogame.getJson());
			}
			if (SottoSoglia.TRUE == videogame.prezzoSottoSoglia(new BigDecimal("10.00"))) {
				toHtml.add(videogame);
			}
			if (videogame.getTipo() != null && Constants.TIPO_TOP.contains(videogame.getTipo().getName())) {
				toHtmlPreferiti.add(videogame);
			}
		}
		toHtml.addAll(separatore);

		for (Videogame videogame : videogameSorted) {
			if (SottoSoglia.ZERO == videogame.prezzoSottoSoglia(new BigDecimal("10.00"))) {
				toHtml.add(videogame);
			}
		}
		for (Videogame videogame : videogameSorted) {
			if (videogame.isPosseduto()) {
				toHtmlPosseduti.add(videogame);
			}
		}

		System.err.println("videogameSorted " + videogameSorted.size() + " toHtmlPosseduti " + toHtmlPosseduti.size());

		output.html(toHtml);

		Collections.sort(toHtmlPosseduti, (a, b) -> a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
		output.htmlPosseduti(toHtmlPosseduti);

		Collections.sort(toHtmlPreferiti, (a, b) -> a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
		output.htmlPreferiti(toHtmlPreferiti);

		output.close();

		// write statistics
		statistics = new PrintWriter(fileStatistics);
		statistics.println("Trovati mid " + trovatiMid);
		statistics.println("Trovati " + trovati);
		statistics.println("# processori " + Runtime.getRuntime().availableProcessors());
		statistics.println("Mid time: " + ((midTime - startTime) / 1000) + " s");
		statistics.println("Total time: " + ((endTime - startTime) / 1000) + " s");
		statistics.println("# threads: " + threads.keySet().size());
		statistics.println("threads counters: " + threads);
		statistics.println("Tipi: " + tipo);
		statistics.println("Generi: " + genere);
		statistics.println("SubGeneri: " + subgenere);
		statistics.println("Tipo di immagini/video type: " + genericDataType);
		statistics.println("Tipo di immagini sub type: " + genericDataSubType);
		statistics.println("Screenshot type: " + screenshot);
		statistics.println("Preview type: " + preview);
		statistics.println("Unknown metadata: " + unKnownMetadata);
		statistics.println("esempi di immagini:"+genericDataSubTypeUrl);
		statistics.close();
		statistics = null;

		System.err.println("\n\n\n\nFINE");
	}

	private void cercaCollegati(final Set<Videogame> videogames) {
		boolean fineRicerca = false;
		while (!fineRicerca) {
			fineRicerca = true;
			int contatore = 0;
			for (Videogame videogame : videogames) {
				contatore += videogame.getOtherIds().size();
			}
			if (Constants.DEBUG) {
				System.err.println("videogames size list " + videogames.size() + " other ids " + contatore);
			}
			final Set<Videogame> tmp = createSet();
			final List<Videogame> errors = createList();
			videogames.parallelStream().forEach(videogame -> {
				videogame.getOtherIds().parallelStream().forEach(id -> {
					addTh();
					final CoppiaUrl coppia = LoadConfig.getCoppia(id);
					final Videogame check = new Videogame(id);
					if (videogames.contains(check) || tmp.contains(check)) {
						return;
					}
					boolean isOk = manageParent(videogames, tmp, videogame, coppia);
					if (!isOk) {
						errors.add(videogame);
					}
				});
			});
			while(!errors.isEmpty()) {
				final Videogame target = errors.remove(0);
				if (target != null) {
					final CoppiaUrl coppia = LoadConfig.getCoppia(target.getId());
					final Videogame check = new Videogame(target.getId());
					if (videogames.contains(check) || tmp.contains(check)) {
						continue;
					}
					System.err.println("Ripeto estrazione di " + coppia.getOriginUrl());
					boolean isOk = manageParent(videogames, tmp, target, coppia);
					if (!isOk) {
						errors.add(target);
					}
				}
			}
			
			fineRicerca = tmp.isEmpty();
			videogames.addAll(tmp);
		}
	}

	private boolean manageParent(final Set<Videogame> videogames, final Set<Videogame> tmp, Videogame videogame,
			final CoppiaUrl coppia) {
		try {
			final Videogame relatedVideogame = new Connection().getVideogame(coppia);
			if (relatedVideogame != null && !videogames.contains(relatedVideogame)) {
				relatedVideogame.setPadre(videogame);
				tmp.add(relatedVideogame);
			}
			return true;
		} catch (IOException e) {
			System.err.println("\n-----------------");
			System.err.println(coppia.getOriginUrl());
			System.err.println(coppia.getJsonUrl());
			System.err.println(e.getClass().getSimpleName() + " : " + e.getMessage());
			System.err.println("\n-----------------");
			return false;
		}
	}

	private static class Writer {
		private final File fileTest = new File("./output.json");
		private final File fileTestEsteso = new File("./output-esteso.json");
		private final String fileTestContent;
		private final String fileTestEstesoContent;
		private final PrintWriter outputMp4;
		private final PrintWriter outputHtml;
		private final PrintWriter outputHtmlPosseduti;
		private final PrintWriter outputHtmlPreferiti;
		private final HtmlTemplate htmlTemplate;

		private Writer() throws IOException {
			fileTestContent = FileUtils.readFileToString(fileTest, Charset.defaultCharset());
			fileTestEstesoContent = FileUtils.readFileToString(fileTestEsteso, Charset.defaultCharset());

			new File("./output.html").renameTo(new File("./output-backup.html"));
			htmlTemplate = new HtmlTemplate();
			outputMp4 = new PrintWriter(new File("./mp4.json"));
			outputHtml = new PrintWriter(new File("./output.html"));
			outputHtmlPosseduti = new PrintWriter(new File("./output-posseduti.html"));
			outputHtmlPreferiti = new PrintWriter(new File("./output-preferiti.html"));
			outputMp4.println("[");
		}

		public void close() {
			outputHtml.close();
			outputHtmlPosseduti.close();
			outputHtmlPreferiti.close();

			outputMp4.println("]");
			outputMp4.close();
		}

		public synchronized void html(final List<Videogame> list) {
			outputHtml.println(htmlTemplate.createHtml(list));
		}

		public synchronized void htmlPosseduti(final List<Videogame> list) {
			outputHtmlPosseduti.println(htmlTemplate.createHtml(list));
		}

		public synchronized void htmlPreferiti(final List<Videogame> list) {
			outputHtmlPreferiti.println(htmlTemplate.createHtml(list));
		}

		public synchronized void mp4(String string) {
			outputMp4.println(string + ",");
		}

		public List<Videogame> read() throws IOException {
			if (Constants.TEST == Test.SI_NORMALE) {
				return fromJSONArray(new JSONArray(fileTestContent));
			} else {
				return fromJSONArray(new JSONArray(fileTestEstesoContent));
			}
		}

		public synchronized void writeClose(final Collection<Videogame> list) throws FileNotFoundException {
			final PrintWriter output = new PrintWriter(fileTest);

			output.print(toJSONArray(list).toString());
			output.close();
			System.err.println("Scritto e chiuso file di test json");
		}

		public synchronized void writeCloseEsteso(final Collection<Videogame> list) throws FileNotFoundException {
			final PrintWriter outputEsteso = new PrintWriter(fileTestEsteso);

			outputEsteso.print(toJSONArray(list).toString());
			outputEsteso.close();
			System.err.println("Scritto e chiuso file di test esteso json");
		}

		private static synchronized JSONArray toJSONArray(final Collection<Videogame> list) {
			final JSONArray array = new JSONArray();
			for (final Videogame videogame : list) {
				array.put(new JSONObject(videogame.getJson()));
			}
			return array;
		}

		private static synchronized List<Videogame> fromJSONArray(final JSONArray list) {
			final List<Videogame> array = Utils.createList();
			for (int index = 0; index < list.length(); index++) {
				final Videogame videogame = Utils.elaboraJson(list.getJSONObject(index));
				if (videogame != null) {
					videogame.setCoppia(LoadConfig.getCoppia(videogame.getId()));
					array.add(videogame);
				}
			}
			return array;
		}

	}

	final Map<Long, Integer> threads = createMap();
	private PrintWriter statistics = null;
	private File fileStatistics = new File("./statistics.txt");
	private final LoadConfig config;
}
