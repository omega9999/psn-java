package it.home.psn;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.SneakyThrows;

public class CheckLogErroriMain {

	@SneakyThrows
	public static void main(String[] args) {
		String[] erroriStr = FileUtils.readFileToString(new File("./z-OUTPUT/errori.txt"), Charset.defaultCharset()).replace("\r", "").split("\n");
		String[] logStr = FileUtils.readFileToString(new File("./z-OUTPUT/log4j-application.log"), Charset.defaultCharset()).replace("\r", "").split("\n");
		for(String errore : erroriStr) {
			for(int index = 0; index < logStr.length; index++) {
				String l = logStr[index];
				if (l != null && l.contains(errore)) {
					logStr[index] = null;
				}
			}
		}
		for(String l : logStr) {
			if (StringUtils.isNotBlank(l)) {
				System.out.println(l);
			}
		}
	}

}
