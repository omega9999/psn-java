package it.home.psn;

import static it.home.psn.Utils.*;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import it.home.psn.module.Videogame;
import it.home.psn.module.Videogame.*;

public class Statistiche {
	private final Map<String, Integer> tipo = createMap();
	private final Map<String, Integer> genere = createMap();
	private final Map<String, Map<String, Integer>> AllMetadata = createMap();
	private final Map<String, Integer> subgenere = createMap();
	private final Map<String, Integer> screenshot = createMap();
	private final Map<String, Integer> preview = createMap();
	private final Map<String, Integer> videos = createMap();
	private final Map<String, Integer> genericDataType = createMap();
	private final Map<String, Integer> genericDataSubType = createMap();
	private final Map<String, String> genericDataSubTypeUrl = createMap();

	
	public void elabora(List<Videogame> videogameSorted) {
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
			for (Video t : videogame.getVideos()) {
				add(videos, t.getType()+"");
			}
			for (Entry<String, List<String>> e : videogame.getAllMetadataValues().entrySet()) {
				for(String t : e.getValue()) {
					add(AllMetadata, e.getKey(), t);
				}
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
	}
	
	
	public void statistics(PrintWriter output) {
		output.println("Tipi: " + tipo);
		output.println("Generi: " + genere);
		output.println("SubGeneri: " + subgenere);
		output.println("Tipo di immagini/video type: " + genericDataType);
		output.println("Tipo di immagini sub type: " + genericDataSubType);
		output.println("Screenshot type: " + screenshot);
		output.println("Preview type: " + preview);
		output.println("\n".repeat(5));
		output.println("Unknown metadata:\n");
		for(Entry<String, Map<String, Integer>> e : AllMetadata.entrySet()) {
			output.println(e);
		}
		output.println("\n".repeat(5));
		output.println("esempi di immagini:\n" + genericDataSubTypeUrl.toString().replace(",", "\n"));
	}
}
