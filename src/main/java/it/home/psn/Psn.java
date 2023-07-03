package it.home.psn;

import static it.home.psn.Utils.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.home.psn.Constants.Test;
import it.home.psn.module.Connection;
import it.home.psn.module.HtmlTemplate;
import it.home.psn.module.LoadConfig;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import it.home.psn.module.Videogame;
import it.home.psn.module.Videogame.SottoSoglia;
import it.home.psn.module.Videogame.Tipo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * https://www.baeldung.com/guide-to-okhttp
 */
@Log4j
public class Psn {
	private static final BigDecimal SOGLIA = new BigDecimal("20.00");
	private static final BigDecimal SCONTO = new BigDecimal("50.00");

	public static void main(String[] args) throws Exception {
		log.info("Inizio");
		final Psn psn = new Psn();
		psn.preferitiInSconto();
		// psn.elenco();
		System.clearProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
		psn.connection.remove();
		log.info("Fine");
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
		Statistiche statistiche = new Statistiche();
		final Set<Videogame> videogames = createSet();
		final long startTime = new Date().getTime();

		final Map<String, Videogame> mappaFromFile = createMap();
		if (Constants.TEST != Test.NO) {
			for (final Videogame videogame : output.readEsteso()) {
				mappaFromFile.put(videogame.getId(), videogame);
			}
		}

		final RelateVideogameManager manager = Constants.TEST == Test.NO ? new RelateVideogameManagerConn(connection)
				: new RelateVideogameManagerFile(mappaFromFile);

		if (Constants.TEST == Test.NO) {
			this.config.getUrls().parallelStream().forEach(coppia -> {
				addTh();
				try {
					final Videogame videogame = ((RelateVideogameManagerConn) manager).getVideogame(coppia);
					if (videogame != null) {
						videogames.add(videogame);
					}
				} catch (IOException e) {
					idErrori.add(coppia.getId());
					log.error("\n-----------------");
					log.error("Problemi di connessione " + e.getMessage() + " " + e.getClass());
					log.error(coppia.getOriginUrl());
					log.error(coppia.getJsonUrl());
					log.error("\n-----------------");
				}
			});
		} else {
			videogames.addAll(output.read());
		}

		final long midTime = new Date().getTime();
		final int trovatiMid = videogames.size();

		if (Constants.TEST == Test.NO) {
			output.writeClose(videogames);
		}
		cercaCollegati(videogames, manager);
		if (Constants.TEST == Test.NO) {
			output.writeCloseEsteso(videogames);
		}

		final long endTime = new Date().getTime();

		final int trovati = videogames.size();

		calcolaAntenati(videogames);

		output.writeSerializzato(videogames);

		final List<Videogame> separatore = Utils.createList();
		final Videogame fake = new Videogame("fake");
		fake.setPadre(fake);
		fake.setAntenato(fake);
		fake.setCoppia(new CoppiaUrl("", "", "", "", "", ""));
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);

		final List<Videogame> videogameSorted = createList();
		videogameSorted.addAll(videogames);
		log.info("videogameSorted.size() " + videogameSorted.size());
		Collections.sort(videogameSorted, (a, b) -> {
			int tmp;

			tmp = a.getPosseduto().compareTo(b.getPosseduto());
			if (tmp != 0) {
				return tmp;
			}

			tmp = Boolean.valueOf(isPrincipalGame(a)).compareTo(isPrincipalGame(b));
			if (tmp != 0) {
				return -1 * tmp;
			}

			tmp = getSconto(a).compareTo(getSconto(b));
			if (tmp != 0) {
				return tmp;
			}
			return a.compareTo(b);
		});


		final List<Videogame> toHtml = Utils.createList();
		final List<Videogame> toHtmlPosseduti = Utils.createList();
		final List<Videogame> toHtmlNonPosseduti = Utils.createList();
		final List<Videogame> toHtmlPreferitiTmp = Utils.createList();
		final List<Videogame> toHtmlPreferiti = Utils.createList();
		final List<Videogame> toHtmlSconto = Utils.createList();
		final List<Videogame> toHtmlScontoDlc = Utils.createList();
		Videogame videogamePrec = videogameSorted.get(0);
		boolean firstSeparatore = false;
		boolean secondSeparatore = false;
		for (Videogame videogame : videogameSorted) {
			if (!videogame.getPosseduto() && videogame.isScontato()) {
				if (isPrincipalGame(videogame)) {
					toHtmlSconto.add(videogame);
				} else {
					toHtmlScontoDlc.add(videogame);
				}
			}
			if (videogame.getJson().contains(".mp4")) {
				// output.mp4(videogame.getJson());
			}
			if (SottoSoglia.TRUE == videogame.prezzoSottoSoglia(SOGLIA)
					|| SottoSoglia.TRUE == videogame.prezzoSottoSconto(SCONTO)
					) {
				if (!firstSeparatore && !videogamePrec.getPosseduto() && videogame.getPosseduto()) {
					toHtml.addAll(separatore);
					firstSeparatore = true;
				}
				if (!secondSeparatore && isPrincipalGame(videogamePrec) && !isPrincipalGame(videogame)) {
					toHtml.addAll(separatore);
					secondSeparatore = true;
				}
				toHtml.add(videogame);
			}
			toHtmlPreferitiTmp.add(videogame);
		}
		toHtml.addAll(separatore);
		toHtml.addAll(separatore);

		for (Videogame videogame : videogameSorted) {
			if (SottoSoglia.ZERO == videogame.prezzoSottoSoglia(SOGLIA)) {
				toHtml.add(videogame);
			}
		}
		for (Videogame videogame : videogameSorted) {
			if (videogame.getPosseduto()) {
				toHtmlPosseduti.add(videogame);
			}
			else {
				Set<Tipo> whiteList = new HashSet<>(videogame.getTipi());
				whiteList.retainAll(List.of(new Tipo("Bundle"), new Tipo("Gioco completo")));
				Set<Tipo> blackList = new HashSet<>(videogame.getTipi());
				blackList.retainAll(List.of(new Tipo("Add-on")));
				if (!whiteList.isEmpty() && blackList.isEmpty()) {
					toHtmlNonPosseduti.add(videogame);
				}
			}
		}

		Collections.sort(toHtmlSconto, (a, b) -> compare(a.getSconto(),b.getSconto()));
		output.htmlSconto(toHtmlSconto);

		Collections.sort(toHtmlScontoDlc, (a, b) -> compare(a.getSconto(),b.getSconto()));
		Collections.sort(toHtmlScontoDlc, (a, b) -> -1 * compare(a.getScontoPerc(),b.getScontoPerc()));
		output.htmlScontoDlc(toHtmlScontoDlc);

		log.info("videogameSorted " + videogameSorted.size() + " toHtmlPosseduti " + toHtmlPosseduti.size());

		output.html(toHtml);

		Collections.sort(toHtmlPosseduti, (a, b) -> compare(a.getName().toLowerCase(),b.getName().toLowerCase()));
		output.htmlPosseduti(toHtmlPosseduti);

		Collections.sort(toHtmlNonPosseduti, (a, b) -> compare(a.getName().toLowerCase(),b.getName().toLowerCase()));
		Collections.sort(toHtmlNonPosseduti, (a, b) -> compare(a.getPriceFull(),b.getPriceFull()));
		output.htmlNonPosseduti(toHtmlNonPosseduti);

		Collections.sort(toHtmlPreferitiTmp, (a, b) -> {
			int tmp;
			tmp = a.getAntenato().getName().toLowerCase().compareTo(b.getAntenato().getName().toLowerCase());
			if (tmp != 0) {
				return tmp;
			}
			if (a.getAntenato().equals(b.getAntenato()) && !a.equals(b)) {
				if (a.getAntenato().equals(a)) {
					return -1;
				}
				if (b.getAntenato().equals(b)) {
					return +1;
				}
				if (a.getPriceFull() != null && b.getPriceFull() != null) {
					tmp = -1 * a.getPriceFull().compareTo(b.getPriceFull());
					if (tmp != 0) {
						return tmp;
					}
				}
			}
			tmp = a.getName().toLowerCase().compareTo(b.getName().toLowerCase());
			if (tmp != 0) {
				return tmp;
			}
			return tmp;
		});
		Videogame ancestorPrec = null;
		for (final Videogame pref : toHtmlPreferitiTmp) {
			if (ancestorPrec != null && !ancestorPrec.equals(pref.getAntenato())) {
				toHtmlPreferiti.add(fake);
			}
			toHtmlPreferiti.add(pref);
			ancestorPrec = pref.getAntenato();
		}
		output.htmlPreferiti(toHtmlPreferiti);

		output.htmlAdult(toHtmlPreferiti.stream().filter(row->row.getGeneriList().contains(new Videogame.Genere("Per adulti"))).collect(Collectors.toList()));

		statistiche.elabora(videogameSorted);
		
		idErrori.forEach(id -> {
			output.errori(id);
			try {
				String response = getAddictionalJson(id, output.erroriJsonResponse);
			}
			catch(Exception e) {
				System.err.println("Id errori " + id);
				e.printStackTrace();
			}
		});
		// write statistics
		output.statistics("Trovati mid " + trovatiMid);
		output.statistics("Trovati " + trovati);
		output.statistics("# processori " + Runtime.getRuntime().availableProcessors());
		output.statistics("Mid time: " + ((midTime - startTime) / 1000) + " s");
		output.statistics("Total time: " + ((endTime - startTime) / 1000) + " s");
		output.statistics("# threads: " + threads.keySet().size());
		output.statistics("threads counters: " + threads);
		output.statistics("\nStatistiche conteggi");
		statistiche.statistics(output.getStatistics());

		output.close();

		log.info("\n\n\n\nFINE");
	}
	
	private static String getAddictionalJson(final String id, PrintWriter erroriJsonResponse) throws IOException {
		final String sha256Hash = LoadConfig.getInstance().getConfig().getProperty("sha256Hash2");
		final String addictionalUrl = LoadConfig.replace(LoadConfig.replace(LoadConfig.getInstance().getConfig().getProperty("base.json.addictional-price.url"), "id", id), "sha256Hash2", sha256Hash);
		final StringBuilder sb = new StringBuilder();
		System.setProperty("java.net.useSystemProxies", "true");
		final OkHttpClient client = new OkHttpClient().newBuilder()
				.callTimeout(60, TimeUnit.SECONDS)
				.connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.build();
		
		final Request request = new Request.Builder().url(addictionalUrl).method("GET", null)
				.addHeader("x-psn-store-locale-override", "IT-IT")
				.build();
		final InputStream stream = client.newCall(request).execute().body().byteStream();

		final BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		String response = sb.toString();
		erroriJsonResponse.println(id);
		erroriJsonResponse.println(addictionalUrl);
		erroriJsonResponse.println(response);
		return response;
	}

	private boolean isPrincipalGame(final Videogame videogame) {
		if (videogame.getTipo() != null && Constants.TIPO_TOP.contains(videogame.getTipo().getName())) {
			return true;
		}
		return false;
	}

	private static BigDecimal getSconto(final Videogame videogame) {
		if (videogame.getSconto() != null && videogame.getSconto().getPrice() != null) {
			return videogame.getSconto().getPrice();
		}
		if (videogame.getPriceFull() != null) {
			return videogame.getPriceFull();
		}
		return new BigDecimal(Long.MAX_VALUE);
	}

	private static final List<String> PRIORITA_ANTENATI = Arrays.asList("Gioco completo", "Gioco", "Gioco PSN",
			"Bundle", "Gioco PS VR", "PS Now");

	private void calcolaAntenati(final Set<Videogame> videogames) {

		final Map<String, Videogame> mappa = createMap();
		for (final Videogame videogame : videogames) {
			mappa.put(videogame.getId(), videogame);
		}

		for (final Videogame videogame : videogames) {
			for (final String id : videogame.getParentIds()) {
				if (mappa.containsKey(id)) {
					videogame.getParentsVideogame().add(mappa.get(id));
				}
			}
		}
		for (final Videogame videogame : videogames) {
			if (!videogame.getParentsVideogame().isEmpty()) {
				final List<Videogame> list = createList();
				list.addAll(videogame.getParentsVideogame());
				Collections.sort(list, (a, b) -> {
					int indexA = PRIORITA_ANTENATI.indexOf(a.getTipoStr());
					int indexB = PRIORITA_ANTENATI.indexOf(b.getTipoStr());
					if (indexA < 0) {
						indexA = Integer.MAX_VALUE;
					}
					if (indexB < 0) {
						indexB = Integer.MAX_VALUE;
					}
					return Integer.compare(indexA, indexB);
				});
				videogame.setAntenato(list.get(0));
			} else {
				videogame.setAntenato(videogame);
			}
		}
		int isRerun = 1;
		final Set<Pair<Videogame, Videogame>> ciclycBreak = createSet();
		while (isRerun > 0) {
			isRerun = 0;
			for (final Videogame videogame : videogames) {
				if (!videogame.getAntenato().equals(videogame.getAntenato().getAntenato())
						&& !videogame.getAntenato().equals(videogame)) {
					final Videogame antenato = videogame.getAntenato().getAntenato();
					final Pair<Videogame, Videogame> pair = Pair.of(videogame, antenato);
					if (!videogame.equals(antenato) && antenato != null && !ciclycBreak.contains(pair)) {
						ciclycBreak.add(pair);
						videogame.setAntenato(antenato);
						isRerun++;
					}
				}
			}
		}
	}

	private void cercaCollegati(final Set<Videogame> videogames, final RelateVideogameManager manager) {
		boolean fineRicerca = false;
		while (!fineRicerca) {
			fineRicerca = true;
			int contatore = 0;
			for (Videogame videogame : videogames) {
				contatore += videogame.getOtherIds().size();
			}
			if (Constants.DEBUG) {
				log.info("videogames size list " + videogames.size() + " other ids " + contatore);
			}
			final Set<Videogame> tmp = createSet();
			final List<Videogame> errors = createList();
			videogames.parallelStream().forEach(videogame -> {
				videogame.getOtherIds().parallelStream().forEach(id -> {
					addTh();

					final Videogame check = new Videogame(id);
					if (videogames.contains(check) || tmp.contains(check)) {
						return;
					}
					boolean isOk = manageParent(manager, videogames, tmp, videogame, id);
					if (!isOk) {
						errors.add(videogame);
					}
				});
			});
			while (!errors.isEmpty()) {
				final Videogame target = errors.remove(0);
				if (target != null) {
					final Videogame check = new Videogame(target.getId());
					if (videogames.contains(check) || tmp.contains(check)) {
						continue;
					}
					log.warn("Ripeto estrazione di " + target.getId());
					boolean isOk = manageParent(manager, videogames, tmp, target, target.getId());
					if (!isOk) {
						errors.add(target);
					}
				}
			}

			fineRicerca = tmp.isEmpty();
			videogames.addAll(tmp);
		}
	}

	private boolean manageParent(final RelateVideogameManager manager, final Set<Videogame> videogames,
			final Set<Videogame> tmp, Videogame videogame, final String id) {
		try {
			final Videogame relatedVideogame = manager.getVideogame(id);
			if (relatedVideogame != null && !videogames.contains(relatedVideogame)) {
				relatedVideogame.setPadre(videogame);
				tmp.add(relatedVideogame);
			}
			return true;
		} catch (IOException e) {
			CoppiaUrl coppia = LoadConfig.getCoppia(id);
			idErrori.add(coppia.getId());
			log.error("\n-----------------");
			log.error(coppia.getOriginUrl());
			log.error(coppia.getJsonUrl());
			log.error(e.getClass().getSimpleName() + " : " + e.getMessage());
			log.error("\n-----------------");
			return false;
		}
	}

	private interface RelateVideogameManager {
		Videogame getVideogame(final String id) throws IOException;
	}

	@RequiredArgsConstructor
	private static class RelateVideogameManagerConn implements RelateVideogameManager {
		@Override
		public Videogame getVideogame(String id) throws IOException {
			return getVideogame(LoadConfig.getCoppia(id));
		}

		public Videogame getVideogame(CoppiaUrl coppia) throws IOException {
			return connection.get().getVideogame(coppia);
		}
		private final ThreadLocal<Connection> connection;
	}

	@RequiredArgsConstructor
	private static class RelateVideogameManagerFile implements RelateVideogameManager {
		private final Map<String, Videogame> mappaFromFile;

		@Override
		public Videogame getVideogame(String id) throws IOException {
			return mappaFromFile.get(id);
		}
	}

	@Getter
	private static class Writer {
		private final File root = new File("./z-OUTPUT");
		private final File fileTest = new File(root, "./output.json");
		private final File fileTestEsteso = new File(root, "./output-esteso.json");
		private final String fileTestContent;
		private final String fileTestEstesoContent;
		private final PrintWriter outputMp4;
		private final PrintWriter outputHtml;
		private final PrintWriter outputHtmlPosseduti;
		private final PrintWriter outputHtmlNonPosseduti;
		private final PrintWriter outputHtmlAdult;
		private final PrintWriter outputHtmlPreferiti;
		private final PrintWriter outputHtmlSconto;
		private final PrintWriter outputHtmlScontoDlc;
		private final PrintWriter statistics;
		private final PrintWriter errori;
		private final PrintWriter erroriJsonResponse;
		private final HtmlTemplate htmlTemplate;

		private final File serializzato = new File(root, "./serializzato-" + Constants.TEST + ".json");

		private Writer() throws IOException {
			fileTestContent = FileUtils.readFileToString(fileTest, Charset.defaultCharset());
			fileTestEstesoContent = FileUtils.readFileToString(fileTestEsteso, Charset.defaultCharset());

			new File(root, "./output.html").renameTo(new File(root, "./output-backup.html"));
			htmlTemplate = new HtmlTemplate();
			outputMp4 = new PrintWriter(new File(root, "./mp4.json"));
			statistics = new PrintWriter(new File(root, "./statistics.txt"));
			errori = new PrintWriter(new File(root, "./errori.txt"));
			erroriJsonResponse = new PrintWriter(new File(root, "./errori-response.json"));
			outputHtml = new PrintWriter(new File(root, "./output.html"));
			outputHtmlPosseduti = new PrintWriter(new File(root, "./output-posseduti.html"));
			outputHtmlNonPosseduti = new PrintWriter(new File(root, "./output-non-posseduti.html"));
			outputHtmlAdult = new PrintWriter(new File(root, "./output-adult.html"));
			outputHtmlPreferiti = new PrintWriter(new File(root, "./output-preferiti.html"));
			outputHtmlSconto = new PrintWriter(new File(root, "./output-sconto.html"));
			outputHtmlScontoDlc = new PrintWriter(new File(root, "./output-sconto-dlc.html"));
			outputMp4.println("[");
		}

		public void close() {
			outputHtml.close();
			outputHtmlPosseduti.close();
			outputHtmlNonPosseduti.close();
			outputHtmlAdult.close();
			outputHtmlPreferiti.close();
			outputHtmlSconto.close();
			outputHtmlScontoDlc.close();
			statistics.close();
			errori.close();
			erroriJsonResponse.close();
			outputMp4.println("]");
			outputMp4.close();
		}

		public void errori(final String str) {
			errori.println(str);
		}

		public void statistics(final String str) {
			statistics.println(str);
		}

		public synchronized void html(final List<Videogame> list) {
			outputHtml.println(htmlTemplate.createHtml(list, "Preferiti in sconto sottosoglia"));
		}

		public synchronized void htmlPosseduti(final List<Videogame> list) {
			outputHtmlPosseduti.println(htmlTemplate.createHtml(list, "Posseduti"));
		}

		public synchronized void htmlNonPosseduti(final List<Videogame> list) {
			outputHtmlNonPosseduti.println(htmlTemplate.createHtml(list, "Non Posseduti"));
		}

		public synchronized void htmlAdult(final List<Videogame> list) {
			outputHtmlAdult.println(htmlTemplate.createHtml(list, "Adult"));
		}

		public synchronized void htmlPreferiti(final List<Videogame> list) {
			outputHtmlPreferiti.println(htmlTemplate.createHtml(list, "Tutti i preferiti"));
		}

		public synchronized void htmlSconto(final List<Videogame> list) {
			outputHtmlSconto.println(htmlTemplate.createHtml(list, "Giochi in sconto"));
		}

		public synchronized void htmlScontoDlc(final List<Videogame> list) {
			outputHtmlScontoDlc.println(htmlTemplate.createHtml(list, "DLC in sconto"));
		}

		public synchronized void mp4(String string) {
			outputMp4.println(string + ",");
		}

		public List<Videogame> read() throws IOException {
			return fromJSONArray(new JSONArray(fileTestContent));
		}

		public List<Videogame> readEsteso() throws IOException {
			return fromJSONArray(new JSONArray(fileTestEstesoContent));
		}

		public synchronized void writeSerializzato(final Collection<Videogame> list) throws IOException {
			final ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

			final DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(" ", "\n");
			final DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
			printer.indentObjectsWith(indenter);
			printer.indentArraysWith(indenter);

			final List<Videogame> tmp = createList();
			tmp.addAll(list);
			Collections.sort(tmp, (a, b) -> compare(a.getId(),b.getId()));
			objectMapper.writer(printer).writeValue(serializzato, tmp);
		}

		public synchronized void writeClose(final Collection<Videogame> list) throws FileNotFoundException {
			final PrintWriter output = new PrintWriter(fileTest);

			output.print(toJSONArray(list).toString());
			output.close();
			log.info("Scritto e chiuso file di test json");
		}

		public synchronized void writeCloseEsteso(final Collection<Videogame> list) throws FileNotFoundException {
			final PrintWriter outputEsteso = new PrintWriter(fileTestEsteso);

			outputEsteso.print(toJSONArray(list).toString());
			outputEsteso.close();
			log.info("Scritto e chiuso file di test esteso json");
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
	
	private static <T extends Comparable<? super T>> int compare(T a, T b) {
		return new SafeComparator<T>().compare(a, b);
	}
	
	private static class SafeComparator<T extends Comparable<? super T>> implements Comparator<T> {

	    @Override
	    public int compare(T o1, T o2) {
	        if (o1 == null && o2 == null) {
	            return 0; // entrambi null, considerati uguali
	        } else if (o1 == null) {
	            return -1; // o1 è null, considerato minore di o2
	        } else if (o2 == null) {
	            return 1; // o2 è null, considerato maggiore di o1
	        } else {
	            return o1.compareTo(o2); // confronto normale dei valori non null
	        }
	    }
	}

	private final Map<Long, Integer> threads = createMap();
	private final Set<String> idErrori = createSet();
	private final ThreadLocal<Connection> connection = ThreadLocal.withInitial(() -> new Connection(idErrori));
	private final LoadConfig config;
}
