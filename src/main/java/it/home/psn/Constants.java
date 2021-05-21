package it.home.psn;

import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
	public static final Test TEST = Test.NO;
	public static final boolean DEBUG = true;
	public static final List<String> TIPO_TOP = Arrays.asList("Gioco completo", "Gioco PSN", "Bundle", "Gioco", "Gioco PS VR",
			"PS Now");

	public enum Test {
		NO, SI_NORMALE, SI_ESTESO;
	}
}
