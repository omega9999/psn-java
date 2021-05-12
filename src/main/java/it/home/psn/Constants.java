package it.home.psn;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
	public static final boolean DEBUG = true;
	
	
	public static boolean isExtended() {
		return TO_STRING_ESTESO;
	}
	
	public static void setExtended(boolean flag) {
		TO_STRING_ESTESO = flag;
	}
	
	
	private static boolean TO_STRING_ESTESO = false;
}
