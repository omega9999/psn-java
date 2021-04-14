package it.home.psn.module;

import static it.home.psn.Utils.compare;
import static it.home.psn.Utils.createList;
import static it.home.psn.Utils.createSet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Set;

import it.home.psn.module.LoadConfig.CoppiaUrl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Videogame implements Comparable<Videogame>{
	public Videogame(String id) {
		this.id = id;
	}

	private String name;
	private final List<Tipo> tipi = createList();
	private final List<Genere> generi = createList();
	private String displayPrizeFull;
	private BigDecimal priceFull;
	private final List<Sconto> sconti = createList();
	private final Set<String> otherIds = createSet();

	private final List<Screenshot> screenshots = createList();
	private final List<Preview> previews = createList();

	private final String id;
	private CoppiaUrl coppia;

	private Videogame padre;

	@Override
	public int compareTo(Videogame obj) {
		int res;
		res = compare(this.getTipo(),obj.getTipo());
		if (res != 0) {
			return res;
		}
		res = compare(this.getName(),obj.getName());
		return res;
	}
	
	public Tipo getTipo() {
		if (getTipi().size() > 0) {
			return getTipi().get(0);
		}
		return null;
	}

	public Genere getGenere() {
		if (getGeneri().size() > 0) {
			return getGeneri().get(0);
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		final Sconto tmp = getSconto();
		if (tmp != null) {
			builder.append(" (" + BigDecimal.ONE.subtract(tmp.getPrice().divide(getPriceFull(), 2, RoundingMode.HALF_UP)).multiply(new BigDecimal(100))+"%)");
			builder.append(" Sconto: ");
			builder.append(tmp);
			builder.append(" , ");
		}
		if (getTipi().size() > 0) {
			builder.append(getTipi().size() == 1 ? getTipi().get(0) : getTipi());
		}

		builder.append(" , ");
		builder.append(getName());
		if (getGeneri().size() > 0) {
			builder.append(" , ");
			builder.append(getGeneri().size() == 1 ? getGeneri().get(0) : getGeneri());
		}
		builder.append(" , prezzo: ");
		builder.append(getPriceFull());

		builder.append(", url: ");
		builder.append(getCoppia().getOriginUrl());
		builder.append(" , ");
		builder.append(getCoppia().getJsonUrl());
		if (getTipi().contains(new Tipo("Gioco completo"))) {
			for(Screenshot screen : getScreenshots()) {
				builder.append("\n\t").append(screen.getUrl());
			}
		}
		return builder.toString().trim();
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
	
	public enum SottoSoglia{
		TRUE,
		FALSE,
		ZERO;
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
			return compare(this.getPrice(),obj.getPrice());
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
			return compare(this.getName(),obj.getName());
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
	public static class Genere implements Comparable<Genere>{
		private String name;
		private int count;
		private String key;

		@Override
		public String toString() {
			return getName();
		}

		@Override
		public int compareTo(Genere obj) {
			return compare(this.getName(),obj.getName());
		}
	}

	@Data
	public static class Screenshot {
		private String type;
		private String typeId;
		private String source;
		private String url;
		private int order;
	}

	@Data
	public static class Preview {
		private String type;
		private String typeId;
		private String source;
		private String url;
		private int order;
		private String streamUrl;
		private final List<String> shots = createList();
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



}