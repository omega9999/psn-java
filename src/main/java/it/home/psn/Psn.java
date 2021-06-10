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
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
import lombok.AllArgsConstructor;

public class Psn {
	private static final BigDecimal SOGLIA = new BigDecimal("16.00");

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

		final Map<String,Videogame> mappaFromFile = createMap(); 
		if (Constants.TEST != Test.NO) {
			for(final Videogame videogame : output.readEsteso()) {
				mappaFromFile.put(videogame.getId(), videogame);
			}
		}
		
		final RelateVideogameManager manager = Constants.TEST == Test.NO ? new RelateVideogameManagerConn() : new RelateVideogameManagerFile(mappaFromFile);
		
		if (Constants.TEST == Test.NO) {
			this.config.getUrls().parallelStream().forEach(coppia -> {
				addTh();
				try {
					final Videogame videogame = ((RelateVideogameManagerConn)manager).getVideogame(coppia);
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
		fake.setCoppia(new CoppiaUrl("", ""));
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);
		separatore.add(fake);

		final List<Videogame> videogameSorted = createList();
		videogameSorted.addAll(videogames);
		System.err.println("videogameSorted.size() "+videogameSorted.size());
		Collections.sort(videogameSorted, (a, b) -> {
			int tmp;

			tmp = a.showScreenshot().compareTo(b.showScreenshot());
			if (tmp != 0) {
				return -1*tmp;
			}
			
			tmp = a.getPosseduto().compareTo(b.getPosseduto());
			if (tmp != 0) {
				return tmp;
			}
			tmp = getSconto(a).compareTo(getSconto(b));
			if (tmp != 0) {
				return tmp;
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
				add(genericDataSubType, t.getTypeData() + "_" + t.getSubTypeData());
				genericDataSubTypeUrl.put(t.getTypeData() + "_" + t.getSubTypeData(), t.getUrl());
			}
		}
		final List<Videogame> toHtml = Utils.createList();
		final List<Videogame> toHtmlPosseduti = Utils.createList();
		final List<Videogame> toHtmlPreferitiTmp = Utils.createList();
		final List<Videogame> toHtmlPreferiti = Utils.createList();
		final List<Videogame> toHtmlSconto = Utils.createList();
		final List<Videogame> toHtmlScontoDlc = Utils.createList();
		for (Videogame videogame : videogameSorted) {
			if (!videogame.getPosseduto() && videogame.isScontato()) {
				if (videogame.getTipo() != null
						&& Constants.TIPO_TOP.contains(videogame.getTipo().getName())) {
					toHtmlSconto.add(videogame);
				}
				else {
					toHtmlScontoDlc.add(videogame);
				}
			}
			if (videogame.getJson().contains(".mp4")) {
				// output.mp4(videogame.getJson());
			}
			if (SottoSoglia.TRUE == videogame.prezzoSottoSoglia(SOGLIA)) {
				toHtml.add(videogame);
			}
			toHtmlPreferitiTmp.add(videogame);
		}
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
		}

		Collections.sort(toHtmlSconto, (a, b) -> a.getSconto().compareTo(b.getSconto()));
		output.htmlSconto(toHtmlSconto);

		Collections.sort(toHtmlScontoDlc, (a, b) -> a.getSconto().compareTo(b.getSconto()));
		Collections.sort(toHtmlScontoDlc, (a, b) -> -1 * a.getScontoPerc().compareTo(b.getScontoPerc()));
		output.htmlScontoDlc(toHtmlScontoDlc);

		System.err.println("videogameSorted " + videogameSorted.size() + " toHtmlPosseduti " + toHtmlPosseduti.size());

		output.html(toHtml);

		Collections.sort(toHtmlPosseduti, (a, b) -> a.getName().toLowerCase().compareTo(b.getName().toLowerCase()));
		output.htmlPosseduti(toHtmlPosseduti);

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
		for(final Videogame pref : toHtmlPreferitiTmp) {
			if (ancestorPrec != null && !ancestorPrec.equals(pref.getAntenato())) {
				toHtmlPreferiti.add(fake);
			}
			toHtmlPreferiti.add(pref);
			ancestorPrec = pref.getAntenato();
		}
		output.htmlPreferiti(toHtmlPreferiti);
		
		
		

		output.close();

		// write statistics
		output.statistics("Trovati mid " + trovatiMid);
		output.statistics("Trovati " + trovati);
		output.statistics("# processori " + Runtime.getRuntime().availableProcessors());
		output.statistics("Mid time: " + ((midTime - startTime) / 1000) + " s");
		output.statistics("Total time: " + ((endTime - startTime) / 1000) + " s");
		output.statistics("# threads: " + threads.keySet().size());
		output.statistics("threads counters: " + threads);
		output.statistics("Tipi: " + tipo);
		output.statistics("Generi: " + genere);
		output.statistics("SubGeneri: " + subgenere);
		output.statistics("Tipo di immagini/video type: " + genericDataType);
		output.statistics("Tipo di immagini sub type: " + genericDataSubType);
		output.statistics("Screenshot type: " + screenshot);
		output.statistics("Preview type: " + preview);
		output.statistics("Unknown metadata: " + unKnownMetadata);
		output.statistics("esempi di immagini:" + genericDataSubTypeUrl);

		System.err.println("\n\n\n\nFINE");
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
	
	private static final List<String> PRIORITA_ANTENATI = Arrays.asList("Gioco completo", "Gioco", "Gioco PSN", "Bundle", "Gioco PS VR","PS Now");
	private void calcolaAntenati(final Set<Videogame> videogames) {
		
		final Map<String,Videogame> mappa = createMap(); 
		for(final Videogame videogame : videogames) {
			mappa.put(videogame.getId(), videogame);
		}
		
		for(final Videogame videogame : videogames) {
			for(final String id : videogame.getParentIds()) {
				if (mappa.containsKey(id)) {
					videogame.getParentsVideogame().add(mappa.get(id));
				}
			}
		}
		for(final Videogame videogame: videogames) {
			if (!videogame.getParentsVideogame().isEmpty()) {
				final List<Videogame> list = createList();
				list.addAll(videogame.getParentsVideogame());
				Collections.sort(list,(a,b)->{
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
			}
			else {
				videogame.setAntenato(videogame);
			}
		}
		int isRerun = 1;
		final Set<Pair<Videogame,Videogame>> ciclycBreak = createSet();
		while(isRerun > 0) {
			isRerun = 0;
			for(final Videogame videogame: videogames) {
				if (!videogame.getAntenato().equals(videogame.getAntenato().getAntenato()) && !videogame.getAntenato().equals(videogame)) {
					final Videogame antenato = videogame.getAntenato().getAntenato();
					final Pair<Videogame,Videogame> pair = Pair.of(videogame,antenato);
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
				System.err.println("videogames size list " + videogames.size() + " other ids " + contatore);
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
					System.err.println("Ripeto estrazione di " + target.getId());
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

	private boolean manageParent(final RelateVideogameManager manager, final Set<Videogame> videogames, final Set<Videogame> tmp, Videogame videogame,
			final String id) {
		try {
			final Videogame relatedVideogame = manager.getVideogame(id);
			if (relatedVideogame != null && !videogames.contains(relatedVideogame)) {
				relatedVideogame.setPadre(videogame);
				tmp.add(relatedVideogame);
			}
			return true;
		} catch (IOException e) {
			System.err.println("\n-----------------");
			System.err.println(LoadConfig.getCoppia(id).getOriginUrl());
			System.err.println(LoadConfig.getCoppia(id).getJsonUrl());
			System.err.println("line 382 Psn " + e.getClass().getSimpleName() + " : " + e.getMessage());
			System.err.println("\n-----------------");
			return false;
		}
	}
	
	private interface RelateVideogameManager{
		Videogame getVideogame(final String id) throws IOException;
	}
	
	private static class RelateVideogameManagerConn implements RelateVideogameManager{
		@Override
		public Videogame getVideogame(String id) throws IOException {
			return getVideogame(LoadConfig.getCoppia(id));
		}
		
		public Videogame getVideogame(CoppiaUrl coppia) throws IOException {
			return new Connection().getVideogame(coppia);
		}
	}
	
	@AllArgsConstructor
	private static class RelateVideogameManagerFile implements RelateVideogameManager{
		private final Map<String, Videogame> mappaFromFile;
		@Override
		public Videogame getVideogame(String id) throws IOException {
			return mappaFromFile.get(id);
		}
	}

	private static class Writer {
		private final File root = new File("./z-OUTPUT");
		private final File fileTest = new File(root,"./output.json");
		private final File fileTestEsteso = new File(root,"./output-esteso.json");
		private final File fileStatistics = new File(root,"./statistics.txt");
		private final String fileTestContent;
		private final String fileTestEstesoContent;
		private final PrintWriter outputMp4;
		private final PrintWriter outputHtml;
		private final PrintWriter outputHtmlPosseduti;
		private final PrintWriter outputHtmlPreferiti;
		private final PrintWriter outputHtmlSconto;
		private final PrintWriter outputHtmlScontoDlc;
		private final PrintWriter statistics;
		private final HtmlTemplate htmlTemplate;
		
		private final File serializzato = new File(root,"./serializzato-"+Constants.TEST+".json");

		private Writer() throws IOException {
			fileTestContent = FileUtils.readFileToString(fileTest, Charset.defaultCharset());
			fileTestEstesoContent = FileUtils.readFileToString(fileTestEsteso, Charset.defaultCharset());

			new File(root,"./output.html").renameTo(new File(root,"./output-backup.html"));
			htmlTemplate = new HtmlTemplate();
			outputMp4 = new PrintWriter(new File(root,"./mp4.json"));
			statistics = new PrintWriter(fileStatistics);
			outputHtml = new PrintWriter(new File(root, "./output.html"));
			outputHtmlPosseduti = new PrintWriter(new File(root, "./output-posseduti.html"));
			outputHtmlPreferiti = new PrintWriter(new File(root,"./output-preferiti.html"));
			outputHtmlSconto = new PrintWriter(new File(root,"./output-sconto.html"));
			outputHtmlScontoDlc = new PrintWriter(new File(root,"./output-sconto-dlc.html"));
			outputMp4.println("[");
		}

		public void close() {
			outputHtml.close();
			outputHtmlPosseduti.close();
			outputHtmlPreferiti.close();
			outputHtmlSconto.close();
			outputHtmlScontoDlc.close();
			statistics.close();
			
			
			outputMp4.println("]");
			outputMp4.close();
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
			Collections.sort(tmp,(a,b)->a.getId().compareTo(b.getId()));
			objectMapper.writer(printer).writeValue(serializzato, tmp);
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
	
	
	private final LoadConfig config;
}
