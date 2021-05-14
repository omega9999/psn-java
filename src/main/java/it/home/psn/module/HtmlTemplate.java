package it.home.psn.module;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

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
	
	private String generaRighe(final List<Videogame> videogames) {
		final StringBuilder sb = new StringBuilder();
		int counter = 0;
		for(final Videogame videogame : videogames) {
			final String id = String.format("id%s",counter++);
			final String immagini = generaImmagini(id, videogame);
			final String video = generaVideo(id, videogame);
			
			sb.append(this.rowTemplate
					.replace("{PREZZO_REF}", videogame.getSconto() != null ? String.valueOf(videogame.getSconto().getPrice()) : "")
					.replace("{SCONTO_REF}", videogame.getScontoPerc() != null ? String.valueOf(videogame.getScontoPerc()) : "")
					.replace("{PREZZO_FULL_REF}", String.valueOf(videogame.getPriceFull()))
					.replace(ID_RIGA_REF, id)
					.replace("{BUTTON_DISPLAY_REF}", (!immagini.isBlank() || !video.isBlank()) ? "inline" : "none")
					.replace("{GIOCO_REF}",videogame.getName())
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
