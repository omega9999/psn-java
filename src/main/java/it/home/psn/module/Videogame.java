package it.home.psn.module;

import static it.home.psn.Utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.home.psn.Constants;
import it.home.psn.module.LoadConfig.CoppiaUrl;
import lombok.*;

@Data
public class Videogame implements Comparable<Videogame> {
	public Videogame(String id) {
		this.id = id;
	}

	private final String id;
	private String name = "";
	
	private String conceptId;

	@JsonIgnore
	private String json = "";

	public String getAntenatoId() {
		return this.antenato != null ? this.antenato.getId() : null;
	}

	public String getPadreId() {
		return this.padre != null ? this.padre.getId() : null;
	}

	public List<String> getParentsVideogameId() {
		final List<String> tmp = createList();
		this.parentsVideogame.stream().forEach(v -> {
			if (v != null) {
				tmp.add(v.getId());
			}
		});
		Collections.sort(tmp);
		return tmp;
	}

	private String description = "";
	@JsonIgnore
	private Videogame padre;
	@JsonIgnore
	private final Set<Videogame> parentsVideogame = createSet();
	@JsonIgnore
	private Videogame antenato;
	
	@Setter(value = AccessLevel.PRIVATE)
	private int bitmaskProblemi = 0;
	
	public void setBitmaskProblemi(Problema value) {
		this.bitmaskProblemi = this.bitmaskProblemi | (0x1 << value.getBit());
	}
	
	private Flag enableVr;
	private Flag requiredVr;
	private Flag online;
	
	public enum Flag{
		TRUE,
		FALSE,
		REQUIRED,
		OPTIONAL;
	}

	@JsonIgnore
	private final Set<Tipo> tipi = createSet();
	@JsonIgnore
	private final Set<Genere> generi = createSet();
	@JsonIgnore
	private final Set<Genere> subgeneri = createSet();
	@JsonIgnore
	private final Set<String> platform = createSet();
	@JsonIgnore
	private final Set<String> allMetadata = createSet();
	private final Map<String, List<String>> allMetadataValues = createMap();
	private String displayPrizeFull;
	private BigDecimal priceFull;
	@JsonIgnore
	private final Set<Sconto> sconti = createSet();
	@JsonIgnore
	private final Set<String> otherIds = createSet();
	@JsonIgnore
	private final Set<String> parentIds = createSet();

	private Boolean posseduto = Boolean.FALSE;

	@JsonIgnore
	private final Set<String> parentUrls = createSet();

	@JsonIgnore
	private final Set<Screenshot> screenshots = createSet();
	@JsonIgnore
	private final Set<Preview> previews = createSet();
	@JsonIgnore
	private final Set<Video> videos = createSet();
	@JsonIgnore
	private final Set<String> voices = createSet();
	@JsonIgnore
	private final Set<String> subtitles = createSet();

	private CoppiaUrl coppia;

	public List<String> getParentsVideogameList() {
		final List<String> tmp = createList();
		this.parentsVideogame.stream().forEach(v->tmp.add(v.getId()));
		Collections.sort(tmp);
		return tmp;
	}

	public List<Tipo> getTipiList() {
		return format(this.tipi);
	}

	public List<Genere> getGeneriList() {
		return format(this.generi);
	}

	public List<Genere> getSubgeneriList() {
		return format(this.subgeneri);
	}


	
	public List<String> getPlatformList() {
		return format(this.platform);
	}

	public List<String> getAllMetadataList() {
		return format(this.allMetadata);
	}

	public Map<String, List<String>> getAllMetadataValues() {
		this.allMetadataValues.values().forEach(Collections::sort);
		return this.allMetadataValues;
	}

	public List<Sconto> getScontiList() {
		return format(this.sconti);
	}

	public List<String> getOtherIdsList() {
		return format(this.otherIds);
	}

	public List<String> getParentIdsList() {
		return format(this.parentIds);
	}

	public List<String> getParentUrlsList() {
		return format(this.parentUrls);
	}

	public List<Screenshot> getScreenshotsList() {
		return format(this.screenshots);
	}

	public List<Preview> getPreviewsList() {
		return format(this.previews);
	}

	public List<Video> getVideosList() {
		return format(this.videos);
	}

	public List<String> getVoicesList() {
		return format(this.voices);
	}

	public List<String> getSubtitlesList() {
		return format(this.subtitles);
	}

	private static <T extends Comparable<? super T>> List<T> format(Set<T> list){
		final List<T> tmp = createList();
		tmp.addAll(list);
		Collections.sort(tmp);
		return tmp;
	}

	public String getTipoStr() {
		final Set<Tipo> objs = getTipi();
		return String.join(", ", join(objs));
	}

	public String getGenereStr() {
		final Set<Genere> objs = getGeneri();
		return String.join(", ", join(objs));
	}

	public String getSubGenereStr() {
		final Set<Genere> objs = getSubgeneri();
		return String.join(", ", join(objs));
	}

	public String getPlatformStr() {
		final Set<String> objs = getPlatform();
		return String.join(", ", join(objs));
	}

	private static String[] join(Collection<?> list) {
		final String[] strs = new String[list.size()];
		int index = 0;
		for (final Object obj : list) {
			strs[index++] = obj.toString();
		}
		return strs;
	}

	@Override
	public int compareTo(Videogame obj) {
		int res;
		res = compare(this.getTipo(), obj.getTipo());
		if (res != 0) {
			return res;
		}
		res = compare(this.getGenere(), obj.getGenere());
		if (res != 0) {
			return res;
		}
		res = compare(this.getName(), obj.getName());
		if (res != 0) {
			return res;
		}
		res = compare(this.getId(), obj.getId());
		return res;
	}

	public Tipo getTipo() {
		if (getTipi().size() > 0) {
			return getTipi().iterator().next();
		}
		return null;
	}

	public Genere getGenere() {
		if (getGeneri().size() > 0) {
			return getGeneri().iterator().next();
		}
		return null;
	}

	public Sconto getSconto() {
		Sconto tmp = null;
		for (Sconto s : getSconti()) {
			if (!s.isPlus() && !s.isEAAccess()) {
				if (tmp == null) {
					tmp = s;
				} else {
					if (tmp.getPrice().compareTo(s.getPrice()) < 0) {
						tmp = s;
					}
				}
			}
		}
		return tmp;
	}

	public BigDecimal getScontoPerc() {
		final Sconto tmp = getSconto();
		if (tmp != null && getPriceFull() != null && getPriceFull().compareTo(BigDecimal.ZERO) != 0) {
			return BigDecimal.ONE.subtract(tmp.getPrice().divide(getPriceFull(), 2, RoundingMode.HALF_UP))
					.multiply(new BigDecimal(100));
		}
		return BigDecimal.ZERO;
	}

	public String getParentUrl() {
		if (!getParentUrls().isEmpty()) {
			return getParentUrls().stream().findFirst().get();
		}
		return getCoppia().getOriginUrl();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		final Sconto tmp = getSconto();
		if (tmp != null) {
			builder.append(" (" + getScontoPerc() + "%)");
			builder.append(" Sconto: ");
			builder.append(tmp);
			builder.append(" , ");
		}
		if (getTipi().size() > 0) {
			builder.append("Tipo ").append(getTipoStr());
		}

		if (getGeneri().size() > 0) {
			builder.append(" , Genere ");
			builder.append(getGenereStr());
		}
		builder.append(" , ");
		builder.append(getName());

		builder.append(" , prezzo: ");
		builder.append(getPriceFull());

		builder.append(", url: ");
		builder.append(getCoppia().getOriginUrl());
		builder.append(" , ");
		builder.append(getCoppia().getJsonUrl());

		return builder.toString().trim();
	}

	public boolean showScreenshot(List<String> strings) {
		for (String str : strings) {
			if (getTipi().contains(new Tipo(str))) {
				return true;
			}
		}
		return false;
	}
	
	public boolean showScreenshot() {
		return showScreenshot(Constants.TIPO_TOP);
	}

	public SottoSoglia prezzoSottoSoglia(final BigDecimal soglia) {
		if (priceFull != null && priceFull.compareTo(BigDecimal.ZERO) == 0) {
			return SottoSoglia.ZERO;
		}
		for (final Sconto sconto : sconti) {
			if (!sconto.isPlus && !sconto.isEAAccess && sconto.getPrice().compareTo(soglia) < 0) {
				return SottoSoglia.TRUE;
			}
		}
		return SottoSoglia.FALSE;
	}
	
	public SottoSoglia prezzoSottoSconto(final BigDecimal scontoSoglia) {
		if (priceFull != null && priceFull.compareTo(BigDecimal.ZERO) == 0) {
			return SottoSoglia.ZERO;
		}
		for (final Sconto sconto : sconti) {
			if (!sconto.isPlus
					&& !sconto.isEAAccess
					&& sconto.getPrice() != null
					&& priceFull != null
					&& scontoSoglia != null
					&& BigDecimal.ONE.subtract(sconto.getPrice().divide(priceFull, RoundingMode.HALF_UP)).compareTo(scontoSoglia.divide(new BigDecimal(100),  RoundingMode.HALF_UP)) >= 0) {
				return SottoSoglia.TRUE;
			}
		}
		return SottoSoglia.FALSE;
	}

	public boolean isScontato() {
		return getSconto() != null;
	}

	public enum SottoSoglia {
		TRUE, FALSE, ZERO;
	}

	@Data
	public static class Sconto implements Comparable<Sconto> {
		private int discount;
		private BigDecimal price;
		private String displayPrice;
		private Date startDate;
		private Date endDate;
		private boolean isPlus;
		private boolean isEAAccess;

		@Override
		public String toString() {
			return getDisplayPrice();
		}

		@Override
		public int compareTo(Sconto obj) {
			return compare(this.getPrice(), obj.getPrice());
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Tipo implements Comparable<Tipo> {
		private String name;
		private String key;

		public Tipo(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return getName();
		}

		@Override
		public int compareTo(Tipo obj) {
			if (!Constants.TIPO_TOP.contains(this.getName()) && Constants.TIPO_TOP.contains(obj.getName())) {
				return +1;
			}
			if (Constants.TIPO_TOP.contains(this.getName()) && !Constants.TIPO_TOP.contains(obj.getName())) {
				return -1;
			}
			return compare(this.getName(), obj.getName());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Tipo other = (Tipo) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
	}

	@Data
	@NoArgsConstructor
	public static class Genere implements Comparable<Genere> {
		private String name;
		private int count;
		private String key;

		public Genere(String name){
			this.name = name;
		}

		@Override
		public String toString() {
			return getName();
		}

		@Override
		public int compareTo(Genere obj) {
			return compare(this.getName(), obj.getName());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Genere other = (Genere) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
	}

	public enum TypeData {
		IMAGE, OTHER_IMAGE, PROMEDIA, SCREENSHOT, PREVIEW, SHOT;
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	private static <T, K extends Comparable<? super K>> int genericCompare(T a, T b, Function<T, ?> ... functions){
		for(final Function<T, ?> function : functions) {
			final K valueA = (K) function.apply(a);
			final K valueB = (K) function.apply(b);
			
			if (valueA == null && valueB == null) {
				return 0;
			}
			if (valueA == null) {
				return -1;
			}
			if (valueB == null) {
				return +1;
			}
			
			final int res = valueA.compareTo(valueB);
			if (res != 0) {
				return res;
			}
		}
		return 0;
	}
	
	@Data
	public static class AbstractUrl {
		private String url;
		private TypeData typeData;
		private String subTypeData;
		private String info;
	}

	@Data
	public static class Screenshot extends AbstractUrl implements Comparable<Screenshot>{
		private String type;
		private String typeId;
		private String source;
		private int order;

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int compareTo(Screenshot arg0) {
			return genericCompare(this, arg0, Screenshot::getOrder, Screenshot::getType, Screenshot::getTypeId, Screenshot::getSource, AbstractUrl::getUrl, AbstractUrl::getTypeData, AbstractUrl::getSubTypeData);
		}
	}

	@Data
	public static class Preview extends AbstractUrl implements Comparable<Preview> {
		private String type;
		private String typeId;
		private String source;
		private int order;
		private String streamUrl;
		private final List<Screenshot> shots = createList();

		public List<Screenshot> getShots() {
			Collections.sort(this.shots);
			return this.shots;
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int compareTo(Preview arg0) {
			return genericCompare(this, arg0, Preview::getOrder, Preview::getType, Preview::getTypeId, Preview::getSource, Preview::getStreamUrl, AbstractUrl::getUrl, AbstractUrl::getTypeData, AbstractUrl::getSubTypeData);
		}
	}

	@Data
	public static class Video extends AbstractUrl implements Comparable<Video>  {
		private static final String CONST = "?country=IT";
		private String type;

		public void setUrl(String url) {
			if (url != null) {
				super.setUrl(url.replace(CONST, ""));
			}
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int compareTo(Video arg0) {
			return genericCompare(this, arg0, Video::getType, AbstractUrl::getSubTypeData, AbstractUrl::getTypeData, AbstractUrl::getUrl);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Videogame other = (Videogame) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public enum Problema{
		PRODUCT_RETRIEVE_NULLO(0),
		SCONTO_NON_EUR(1),
		RATIO_SCONTO_NAN(2),
		CONCEPT_ID_NULLO(3),
		IMMAGINI_AGGIUNTIVE_ERRORE(4),
		PREZZO_AGGIUNTIVO_ERRORE(5),
		;
		
		private final int bit;
	}

}