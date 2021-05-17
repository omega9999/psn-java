package it.home.psn.module;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

import it.home.psn.Utils;
import it.home.psn.module.Videogame.Screenshot;
import it.home.psn.module.Videogame.Video;

public class HtmlTemplate {

	
	public HtmlTemplate() throws IOException {
		this.template = getFile("template.html");
		this.rowTemplate = getFile("row-template.html");
		this.immagineTemplate = getFile("immagine-template.html");
		this.videoTemplate = getFile("video-template.html");
	}
	
	public String createHtml(final List<Videogame> videogames) {
		return this.template.replace("{REPLACE_RIGHE_REF}", generaRighe(videogames));
	}
	
	private String elabMetadati(final Videogame videogame) {
		final StringBuilder visibile = new StringBuilder();
		final List<String> tooltip = Utils.createList();
		if(videogame.isPosseduto()) {
			visibile.append("[X] ");
			tooltip.add("Posseduto");
		}
		
		if (!videogame.getVoices().isEmpty()) {
			tooltip.add(String.format("Lingue disponibili: %s", String.join(", ", videogame.getVoices().toArray(new String[0]))));
			if (videogame.getVoices().contains("it")) {
				visibile.append("[vIT] ");
			}
			else if (videogame.getVoices().contains("en")) {
				visibile.append("[vEN] ");
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
		
		
		if(Boolean.TRUE.equals(videogame.getEnableVr())) {
			visibile.append("[VR] ");
			tooltip.add("VR disponibile");
		}
		if(Boolean.TRUE.equals(videogame.getRequiredVr())) {
			visibile.append("[VR+] ");
			tooltip.add("VR obbligatoria");
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

		final StringBuilder sb = new StringBuilder();
		if (!visibile.toString().trim().isEmpty()) {
			sb.append("<div class='tooltip'>").append(visibile.toString()).append("<span class='tooltiptext tooltip-right'>").append(String.join("<br/>", tooltip).trim()).append("</span></div>");
		}
		return sb.toString().trim();
	}
	
	private String generaRighe(final List<Videogame> videogames) {
		final StringBuilder sb = new StringBuilder();
		int counter = 0;
		for(final Videogame videogame : videogames) {
			final String id = String.format("id%s",counter++);
			final String immagini = generaImmagini(id, videogame);
			final String video = generaVideo(id, videogame);
			
			sb.append(this.rowTemplate
					.replace("{PREZZO_REF}", videogame.getSconto() != null ? "&euro; " + videogame.getSconto().getPrice() : "")
					.replace("{SCONTO_REF}", videogame.getScontoPerc() != null ? String.valueOf(videogame.getScontoPerc()) + " %" : "")
					.replace("{PREZZO_FULL_REF}", "&euro; " + videogame.getPriceFull())
					.replace("{METADATA_REF}", elabMetadati(videogame))
					.replace(ID_RIGA_REF, id)
					.replace("{BUTTON_DISPLAY_REF}", (!immagini.isBlank() || !video.isBlank()) ? "inline" : "none")
					.replace("{GIOCO_REF}",StringEscapeUtils.escapeHtml4(videogame.getName().trim()))
					.replace("{IMMAGINI_REF}", immagini)
					.replace("{VIDEOS_REF}", video)
					.replace("{TIPO_REF}", videogame.getTipoStr())
					.replace("{GENERE_REF}",  videogame.getGenereStr())
					.replace("{SUBGENERE_REF}",  videogame.getSubGenereStr())
					.replace("{POSSEDUTO_REF}", videogame.isPosseduto() ? "(X) ":"")
					.replace("{LINK_ORIG_REF}", videogame.getCoppia().getOriginUrl())
					.replace("{LINK_PARENT_REF}", videogame.getParentUrl())
					.replace("{LINK_JSON_REF}", videogame.getCoppia().getJsonUrl())
					);
		}
		return sb.toString();
	}
	
	private String generaImmagini(final String id, final Videogame videogame) {
		final StringBuilder sb = new StringBuilder();
		for(Screenshot screen : videogame.getScreenshots()) {
			sb.append(this.immagineTemplate
					.replace(ID_RIGA_REF, id)
					.replace("{IMMAGINE_REF}", screen.getUrl()));
		}
		return sb.toString();
	}
	private String generaVideo(final String id, final Videogame videogame) {
		final StringBuilder sb = new StringBuilder();
		for(Video screen : videogame.getVideos()) {
			sb.append(this.videoTemplate
					.replace(ID_RIGA_REF, id)
					.replace("{VIDEO_REF}", screen.getUrl()));
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
}
