package it.home.psn;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Application {
    private static final String PARAMETRI_PREVISTI = Stream.of(Parametri.values()).map(Parametri::getParametro).collect(Collectors.joining(" "));
    public static void main(String[] args) {
        if (args.length == 0){
            log.error("\nMancano i parametri, previsti: " + PARAMETRI_PREVISTI);
        }
        else{
            for (String arg : args){
                var param = Parametri.getByValue(arg);
                param.getWorker().run();
            }
        }

    }

    @Getter
    @RequiredArgsConstructor
    private enum Parametri{
        PSN("psn", new Psn()),
        SISTEMA_PREFERITI("fix", new SistemaPreferiti.Work())
        ;

        private final String parametro;
        private final Runnable worker;

        public static Parametri getByValue(String value) {
            return Arrays.stream(Parametri.values())
                    .filter(row -> row.getParametro().equals(value))
                    .findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Parametro non valido: " + value + ", previsti: " + PARAMETRI_PREVISTI));
        }
    }
}
