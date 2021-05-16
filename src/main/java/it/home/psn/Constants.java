package it.home.psn;

import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
	public static final boolean DEBUG = true;
	public static List<String> TIPO_TOP = Arrays.asList("Gioco completo","Gioco PSN","Bundle","Gioco","Gioco PS VR","PS Now");
	
	public static boolean isExtended() {
		return TO_STRING_ESTESO;
	}
	
	public static void setExtended(boolean flag) {
		TO_STRING_ESTESO = flag;
	}
	
	
	private static boolean TO_STRING_ESTESO = false;
}
