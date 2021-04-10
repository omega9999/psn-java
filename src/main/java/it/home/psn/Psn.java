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
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(3 - 1));

		pw = new PrintWriter(file);
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
		final Map<String, Integer> screenshot = createMap();
		final Map<String, Integer> preview = createMap();
		for (Videogame videogame : videogameSorted) {
			for (Genere g : videogame.getGeneri()) {
				add(genere, g.getName());
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
		}
		for (Videogame videogame : videogameSorted) {
			if (SottoSoglia.TRUE == videogame.prezzoSottoSoglia(new BigDecimal("10.00"))) {
				pw.println(videogame + " " + videogame.getCoppia().getOriginUrl());
			}
		}
		pw.println("\n\n\n\n\n");
		for (Videogame videogame : videogameSorted) {
			if (SottoSoglia.ZERO == videogame.prezzoSottoSoglia(new BigDecimal("10.00"))) {
				pw.println(videogame + " " + videogame.getCoppia().getOriginUrl());
			}
		}

		pw.close();
		pw = null;

		System.err.println("\n\n\n\n");
		System.err.println("Trovati mid " + trovatiMid);
		System.err.println("Trovati " + trovati);

		System.err.println("# processori " + Runtime.getRuntime().availableProcessors());
		System.err.println("Mid time: " + ((midTime - startTime) / 1000) + " s");
		System.err.println("Total time: " + ((endTime - startTime) / 1000) + " s");
		System.err.println("# threads: " + threads.keySet().size());
		System.err.println("threads counters: " + threads);
		System.err.println("Tipi: " + tipo);
		System.err.println("Generi: " + genere);
		System.err.println("Screenshot type: " + screenshot);
		System.err.println("Preview type: " + preview);
	}

	@SuppressWarnings("unused")
	private void elenco() throws Exception {
		final Set<Videogame> videogames = createSet();
		pw = new PrintWriter(file);
		// this.config.checkPreferiti();

		for (String idRicerca : this.config.getRicercheIds()) {
			final List<Videogame> tmp = new Connection()
					.ricerca("https://store.playstation.com/chihiro-api/viewfinder/IT/it/999/" + idRicerca);
			videogames.addAll(tmp);
			System.err.println(idRicerca + " size set " + tmp.size());
		}

		// videogames.addAll(new
		// Connection().ricerca("https://store.playstation.com/chihiro-api/viewfinder/IT/it/999/STORE-MSF75508-GAMELATEST?size=30&gkb=1&start=0&game_content_type=games"));

		// cercaCollegati(videogames);

		System.err.println("\n\n\n\n");
		System.err.println("Trovati " + videogames.size());

		final List<Videogame> videogameSorted = Arrays.asList(videogames.toArray(new Videogame[0]));
		Collections.sort(videogameSorted, (obj1, obj2) -> obj1.getName().compareTo(obj2.getName()));

		final Set<String> tipo = createSet();
		final Set<String> genere = createSet();
		final Set<String> screenshot = createSet();
		final Set<String> preview = createSet();
		for (Videogame videogame : videogameSorted) {
			pw.println(videogame);
			for (Genere g : videogame.getGeneri()) {
				genere.add(g.getName());
			}
			for (Tipo t : videogame.getTipi()) {
				tipo.add(t.getName());
			}
			for (Screenshot t : videogame.getScreenshots()) {
				screenshot.add(t.getType());
			}
			for (Preview t : videogame.getPreviews()) {
				preview.add(t.getType());
			}
		}

		pw.close();
		pw = null;

		System.err.println("Tipi: " + tipo);
		System.err.println("Generi: " + genere);
		System.err.println("Screenshot type: " + screenshot);
		System.err.println("Preview type: " + preview);
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
						System.err.println(e.getClass().getSimpleName() + " : " + e.getMessage());
					}
				});
			});
			fineRicerca = tmp.isEmpty();
			videogames.addAll(tmp);
		}
	}

	final Map<Long, Integer> threads = createMap();
	private PrintWriter pw = null;
	private File file = new File("./output.txt");
	private final LoadConfig config;
}
