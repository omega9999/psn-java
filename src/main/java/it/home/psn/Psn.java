package it.home.psn;

import static it.home.psn.Utils.add;
import static it.home.psn.Utils.createMap;
import static it.home.psn.Utils.createSet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.home.psn.module.Connection;
import it.home.psn.module.HtmlTemplate;
import it.home.psn.module.LoadConfig;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import it.home.psn.module.Videogame;
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
		//System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(3 - 1));

		Writer output = new Writer();
		
		this.config.checkPreferiti();

		final Set<Videogame> videogames = createSet();
		final long startTime = new Date().getTime();
		this.config.getUrls().parallelStream().forEach(coppia -> {
			addTh();
			try {
				final Videogame videogame = new Connection().getVideogame(coppia);
				if (videogame != null) {
					videogames.add(videogame);
				}
			} catch (IOException e) {
				System.err.println("Problemi di connessione");
				System.err.println(coppia.getOriginUrl());
				System.err.println(coppia.getJsonUrl());
				e.printStackTrace();
			}
		});

		final long midTime = new Date().getTime();
		final int trovatiMid = videogames.size();

		cercaCollegati(videogames);

		final long endTime = new Date().getTime();

		final int trovati = videogames.size();

		final List<Videogame> videogameSorted = Arrays.asList(videogames.toArray(new Videogame[0]));
		Collections.sort(videogameSorted);

		final Map<String, Integer> tipo = createMap();
		final Map<String, Integer> genere = createMap();
		final Map<String, Integer> unKnownMetadata = createMap();
		final Map<String, Integer> subgenere = createMap();
		final Map<String, Integer> screenshot = createMap();
		final Map<String, Integer> preview = createMap();
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
		}
		final List<Videogame> toHtml = Utils.createList();
		final List<Videogame> toHtmlPosseduti = Utils.createList();
		for (Videogame videogame : videogameSorted) {
			if (videogame.getJson().contains(".mp4")) {
				output.mp4(videogame.getJson());
			}
			if (SottoSoglia.TRUE == videogame.prezzoSottoSoglia(new BigDecimal("10.00"))) {
				output.println(videogame);
				toHtml.add(videogame);
				//output.println(videogame.getCoppia().getOriginUrl());
			}
		}
		output.println("\n\n\n\n\n");
		for (Videogame videogame : videogameSorted) {
			if (SottoSoglia.ZERO == videogame.prezzoSottoSoglia(new BigDecimal("10.00"))) {
				output.println(videogame);
				//output.println(videogame.getCoppia().getOriginUrl());
			}
		}
		for (Videogame videogame : videogameSorted) {
			if (videogame.isPosseduto()) {
				toHtmlPosseduti.add(videogame);
			}
		}

		System.err.println("videogameSorted "+videogameSorted.size()+" toHtmlPosseduti "+ toHtmlPosseduti.size());
		
		output.html(toHtml);

		Collections.sort(toHtmlPosseduti, (a,b)->a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
		output.htmlPosseduti(toHtmlPosseduti);
		
		
		output.close();

		
		//write statistics
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
		statistics.println("Screenshot type: " + screenshot);
		statistics.println("Preview type: " + preview);
		statistics.println("Unknown metadata: " + unKnownMetadata);
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
			videogames.parallelStream().forEach(videogame -> {
				videogame.getOtherIds().parallelStream().forEach(id -> {
					addTh();
					final CoppiaUrl coppia = LoadConfig.getCoppia(id);
					final Videogame check = new Videogame(id);
					if (videogames.contains(check) || tmp.contains(check)) {
						return;
					}
					try {
						final Videogame relatedVideogame = new Connection().getVideogame(coppia);
						if (relatedVideogame != null && !videogames.contains(relatedVideogame)) {
							relatedVideogame.setPadre(videogame);
							tmp.add(relatedVideogame);
						}
					} catch (IOException e) {
						System.err.println(coppia.getOriginUrl());
						System.err.println(coppia.getJsonUrl());
						System.err.println(e.getClass().getSimpleName() + " : " + e.getMessage());
					}
				});
			});
			fineRicerca = tmp.isEmpty();
			videogames.addAll(tmp);
		}
	}
	
	private static class Writer{
		private final PrintWriter output;
		private final PrintWriter outputEsteso;
		private final PrintWriter outputMp4;
		private final PrintWriter outputHtml;
		private final PrintWriter outputHtmlPosseduti;
		private final HtmlTemplate htmlTemplate;
		
		private Writer() throws IOException {
			new File("./output.html").renameTo(new File("./output-backup.html"));
			htmlTemplate = new HtmlTemplate();
			output = new PrintWriter(new File("./output.txt"));
			outputEsteso = new PrintWriter(new File("./output-esteso.txt"));
			outputMp4 = new PrintWriter(new File("./mp4.json"));
			outputHtml = new PrintWriter(new File("./output.html"));
			outputHtmlPosseduti = new PrintWriter(new File("./output-posseduti.html"));
			outputMp4.println("[");
		}

		public void close() {
			output.close();
			outputEsteso.close();
			outputHtml.close();
			outputHtmlPosseduti.close();
			
			outputMp4.println("]");
			outputMp4.close();
		}
		
		public synchronized void html(final List<Videogame> list) {
			outputHtml.println(htmlTemplate.createHtml(list));
		}
		public synchronized void htmlPosseduti(final List<Videogame> list) {
			outputHtmlPosseduti.println(htmlTemplate.createHtml(list));
		}
		
		public synchronized void mp4(String string) {
			outputMp4.println(string+",");
		}

		public synchronized void println(Object string) {
			Constants.setExtended(false);
			output.println(string);
			Constants.setExtended(true);
			outputEsteso.println(string);
			Constants.setExtended(false);
		}
		
	}

	final Map<Long, Integer> threads = createMap();
	private PrintWriter statistics = null;
	private File fileStatistics = new File("./statistics.txt");
	private final LoadConfig config;
}
