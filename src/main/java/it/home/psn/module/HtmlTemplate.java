package it.home.psn.module;

import static it.home.psn.Utils.createList;
import static it.home.psn.Utils.createSet;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import it.home.psn.Utils;
import it.home.psn.module.Videogame.AbstractUrl;
import it.home.psn.module.Videogame.Flag;
import it.home.psn.module.Videogame.Preview;
import it.home.psn.module.Videogame.Sconto;
import it.home.psn.module.Videogame.TypeData;

public class HtmlTemplate {

	
	public HtmlTemplate() throws IOException {
		this.template = getFile("template.html");
		this.rowTemplate = getFile("row-template.html");
		this.immagineTemplate = getFile("immagine-template.html");
		this.videoTemplate = getFile("video-template.html");
	}
	
	public String createHtml(final List<Videogame> videogames, final String titolo) {
		return this.template
				.replace("{TITOLO_REF}", titolo)
				.replace("{REPLACE_RIGHE_REF}", generaRighe(videogames));
	}
	
	private String elabMetadati(final Videogame videogame) {
		final StringBuilder visibile = new StringBuilder();
		final List<String> tooltip = Utils.createList();
		if(videogame.getPosseduto().booleanValue()) {
			visibile.append("[X] ");
			tooltip.add("Posseduto ");
		}
		if(!videogame.getCoppia().getFiles().isEmpty()) {
			visibile.append("[*] ");
			tooltip.add("Link Originale: " + String.join(", ", videogame.getCoppia().getFiles()) + " ");
		}
		
		if (!videogame.getVoices().isEmpty()) {
			tooltip.add(String.format("Lingue disponibili: %s", String.join(", ", videogame.getVoices().toArray(new String[0]))));
			if (videogame.getVoices().contains("it")) {
				visibile.append("[vIT] ");
			}
			else if (videogame.getVoices().contains("en")) {
				visibile.append("[vEN] ");
			}
			else if (videogame.getVoices().contains("ja")) {
				visibile.append("[vJA] ");
			}
			else {
				visibile.append("[vOT] ");
			}
		}
		if (!videogame.getSubtitles().isEmpty()) {
			tooltip.add(String.format("Sottotitoli disponibili: %s", String.join(", ", videogame.getSubtitles().toArray(new String[0]))));	
			if (videogame.getSubtitles().contains("it")) {
				visibile.append("[sIT] ");
			}
			else if (videogame.getSubtitles().contains("en")) {
				visibile.append("[sEN] ");
			}
			else {
				visibile.append("[sOT] ");
			}
		}
		
		
		if(Flag.TRUE.equals(videogame.getEnableVr())) {
			visibile.append("[VR] ");
			tooltip.add("VR disponibile");
		}
		if(Flag.TRUE.equals(videogame.getRequiredVr())) {
			visibile.append("[VR+] ");
			tooltip.add("VR obbligatoria");
		}
		if(Flag.REQUIRED.equals(videogame.getOnline())) {
			visibile.append("[ON+] ");
			tooltip.add("Online obbligatorio");
		}
		if(Flag.OPTIONAL.equals(videogame.getOnline())) {
			visibile.append("[ON] ");
			tooltip.add("Online opzionale");
		}
		
		if (!videogame.getUnKnownMetadataValues().isEmpty()) {
			visibile.append("[?] ");
			if (!tooltip.isEmpty()) {
				tooltip.add("");
				tooltip.add("Altri metadati:");
			}
			for(Entry<String,List<String>>entry:videogame.getUnKnownMetadataValues().entrySet()) {
				tooltip.add(entry.getKey() + ": " + String.join(", ", entry.getValue().toArray(new String[0])));
			}
		}
		if (videogame.getBitmaskProblemi() != 0) {
			visibile.append("[!] ");
			tooltip.add("Bitmask url sconti: " + videogame.getBitmaskProblemi());
		}
		
		if (!videogame.getAntenato().equals(videogame)) {
			if (!tooltip.isEmpty()) {
				tooltip.add("");
			}
			tooltip.add("Antenato: " + videogame.getAntenato().getName());
		}

		final StringBuilder sb = new StringBuilder();
		if (visibile.toString().trim().isEmpty() && !tooltip.isEmpty()) {
			visibile.append("&nbsp;");
		}
		if (!visibile.toString().trim().isEmpty()) {
			sb.append("<div class='tooltip'>").append(visibile.toString()).append("<span class='tooltiptext tooltip-right'>").append(String.join("<br/>", tooltip).trim()).append("</span></div>");
		}
		return sb.toString().trim();
	}
	
	private String getDescription(final Videogame videogame) {
		final String name = StringEscapeUtils.escapeHtml4(videogame.getName().trim());
		final StringBuilder sb = new StringBuilder();
		if (!StringUtils.isBlank(videogame.getDescription())) {
			sb.append("<div class='tooltip'>").append(name).append("<span class='tooltiptext tooltip-right' style='width: 1000px;'>").append(clearDesctipton(videogame.getDescription())).append("</span></div>");
		}
		else {
			sb.append(name);
		}
		return sb.toString().trim();
	}
	
	private String clearDesctipton(final String description) {
		//StringEscapeUtils.escapeHtml4
		String tmp = description;
		for(final String str : REMOVE_DESCRIPTION) {
			tmp = tmp.replace(str, "");
		}
		tmp = tmp.replace("●", "• ");
		tmp = tmp.replace("<br/>", "<br>");
		tmp = tmp.replace("<br />", "<br>");
		tmp = tmp.replace("\s+<br>", "<br>");
		tmp = tmp.replaceAll("<br>\s+", "<br>");
		tmp = removeDuplicate(tmp,"<br><br><br>", "<br><br>");
		tmp = trimHtml(tmp, "<br>");
		return tmp;
	}
	
	private String removeDuplicate(final String str, final String target, final String replace) {
		String tmp = str.trim();
		while(tmp.contains(target)) {
			tmp = tmp.replace(target, replace);
		}
		return tmp;
	}
	
	private String trimHtml(final String str, final String target) {
		String tmp = str.trim();
		while (tmp.startsWith(target)) {
			tmp = tmp.replaceAll("^" + target, "").trim();
		}
		while (tmp.endsWith(target)) {
			tmp = tmp.replaceAll(target + "$", "").trim();
		}
		return tmp;
	}
	
	private static boolean printImporto(final BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) != 0;
	}
	
	private boolean printImporto(Sconto sconto) {
		return sconto != null && printImporto(sconto.getPrice());
	}
	
	private String generaRighe(final List<Videogame> videogames) {
		final StringBuilder sb = new StringBuilder();
		int counter = 0;
		for(final Videogame videogame : videogames) {
			final String id = String.format("id%s",counter++);
			final String immagini = generaImmagini(id, videogame);
			final String video = generaVideo(id, videogame);
			
			sb.append(this.rowTemplate
					.replace("{PREZZO_REF}", printImporto(videogame.getSconto()) ? "&euro; " + videogame.getSconto().getPrice() : "")
					.replace("{SCONTO_REF}", printImporto(videogame.getScontoPerc()) ? String.valueOf(videogame.getScontoPerc()) + " %" : "")
					.replace("{PREZZO_FULL_REF}", printImporto(videogame.getPriceFull()) ? "&euro; " + videogame.getPriceFull() : "")
					.replace("{METADATA_REF}", elabMetadati(videogame))
					.replace("{CONSOLE_REF}", StringEscapeUtils.escapeHtml4(videogame.getPlatformStr()))
					.replace(ID_RIGA_REF, id)
					.replace("{BUTTON_DISPLAY_REF}", (!immagini.isBlank() || !video.isBlank()) ? "inline" : "none")
					.replace("{GIOCO_REF}",getDescription(videogame))
					.replace("{IMMAGINI_REF}", immagini)
					.replace("{VIDEOS_REF}", video)
					.replace("{TIPO_REF}", videogame.getTipoStr())
					.replace("{GENERE_REF}",  videogame.getGenereStr())
					.replace("{SUBGENERE_REF}",  videogame.getSubGenereStr())
					.replace("{POSSEDUTO_REF}", Boolean.TRUE.equals(videogame.getPosseduto()) ? "(X) ":"")
					.replace("{LINK_ORIG_REF}", videogame.getCoppia().getOriginUrl())
					.replace("{LINK_PARENT_REF}", videogame.getParentUrl())
					.replace("{LINK_JSON_REF}", videogame.getCoppia().getJsonUrl())
					.replace("{LINK_PRICE_JSON_REF}", videogame.getCoppia().getPriceJsonUrl())
					.replace("{LINK_AD_PRICE_JSON_REF}", videogame.getCoppia().getAddictionalJsonUrl())
					.replace("{LINK_IMAGE_JSON_REF}", videogame.getCoppia().getImageJsonUrl(videogame))
					);
		}
		return sb.toString();
	}
	
	private static final List<String> VIDEO = Arrays.asList("mp4");
	private static final List<TypeData> PRIORITA = Arrays.asList(TypeData.IMAGE,TypeData.SCREENSHOT,TypeData.SHOT,TypeData.PREVIEW, TypeData.OTHER_IMAGE);
	private Collection<AbstractUrl> getData(final Videogame videogame, final GraficaType type){
		final Set<AbstractUrl> data = createSet();
		final List<AbstractUrl> res = createList();
		CollectionUtils.addAll(data, videogame.getScreenshots());
		CollectionUtils.addAll(data, videogame.getVideos());
		CollectionUtils.addAll(data, videogame.getPreviews());
		
		for(Preview p : videogame.getPreviews()) {
			CollectionUtils.addAll(data,p.getShots()); 
		}
		for (AbstractUrl t : data) {
			final String[] str = t.getUrl().split("\\.");
			final String extension =  str[str.length - 1].toLowerCase();
			if (type == GraficaType.VIDEO && VIDEO.contains(extension)) {
				res.add(t);
			}
			if(type == GraficaType.IMMAGINE && !VIDEO.contains(extension)) {
				res.add(t);
			}
		}
		Collections.sort(res, (a,b)->{
			int indexA = PRIORITA.indexOf(a.getTypeData());
			int indexB = PRIORITA.indexOf(b.getTypeData());
			return Integer.compare(indexA, indexB);
		});
		return res;
	}
	
	private enum GraficaType{
		IMMAGINE, VIDEO;
	}
	
	private String generaImmagini(final String id, final Videogame videogame) {
		final StringBuilder sb = new StringBuilder();
		for(AbstractUrl url : getData(videogame, GraficaType.IMMAGINE)) {
			sb.append(this.immagineTemplate
					.replace(ID_RIGA_REF, id)
					.replace("{IMMAGINE_TYPE_REF}", url.getTypeData()+url.getSubTypeData())
					.replace("{IMMAGINE_REF}", url.getUrl()));
		}
		return sb.toString();
	}
	private String generaVideo(final String id, final Videogame videogame) {
		final StringBuilder sb = new StringBuilder();
		for(AbstractUrl url : getData(videogame, GraficaType.VIDEO)) {
			sb.append(this.videoTemplate
					.replace(ID_RIGA_REF, id)
					.replace("{VIDEO_REF}", url.getUrl()));
		}
		return sb.toString();
	}
	
	private String getFile(String fileName) throws IOException {
		final ClassLoader classLoader = getClass().getClassLoader();
		try(final InputStream inputStream = classLoader.getResourceAsStream("html/"+fileName)){
			return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		}
	}
	
	
	private final String template;
	private final String rowTemplate;
	private final String immagineTemplate;
	private final String videoTemplate;
	
	private static final String ID_RIGA_REF = "{ID_RIGA_REF}";
	
	private static final String[] REMOVE_DESCRIPTION = {
			"Per giocare a questo gioco su PS5, è possibile che tu debba aggiornare il software di sistema alla versione più recente.",
			"Anche se questo gioco è utilizzabile su PS5, alcune funzioni disponibili su PS4 potrebbero non risultare disponibili.",
			"Per ulteriori informazioni, consulta la pagina PlayStation.com/bc.",
			"Prima di usare questo prodotto, leggere attentamente le Avvertenze per la salute.",
			"Library programs ©Sony Interactive Entertainment Inc. concesso in licenza esclusivamente a Sony Interactive Entertainment Europe.",
			"Si applicano i Termini d'uso del software.",
			"Si consiglia di visitare eu.playstation.com/legal per i diritti di utilizzo completi.",
			"Il download del presente prodotto è soggetto ai Termini di servizio e alle Condizioni d'uso del software di PlayStation Network e a qualsiasi altra condizione supplementare specifica applicabile a questo articolo.",
			"Se non si desidera accettare questi Termini, non scaricare questo articolo.",
			"Per maggiori dettagli, consultare i Termini di Servizio.",
			"Una tantum applicabile per scaricare su più sistemi PS4.",
			"Si può utilizzare su PS4 pincipale senza effettuare l'accesso a PlayStation Network; l'accesso va effettuato per l'uso su altri sistemi PS4.",
			"<b></b>",
			"<b>  </b>"};
}
