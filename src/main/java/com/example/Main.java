package com.example;


import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        String zone = null;
        LocalDate date = LocalDate.now();
        String charging = "2h";
        boolean sorted = false;
        boolean help = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    help = true;
                    break;
                case "--zone":
                    if (i + 1 < args.length) {
                        zone = args[++i];
                    } else {
                        System.out.println("Fel: --zone kräver ett värde (SE1|SE2|SE3|SE4)");
                        return;
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        date = LocalDate.parse(args[++i]);
                    } else {
                        System.out.println("Fel: datum formatet måte vara (YYYY-MM-DD)");
                        return;}
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        charging = args[++i];
                    } else {
                        System.out.println("Fel: tider som finns (2h|4h|8h)");
                        return;
                    }
                    break;
                case "--sorted":
                    sorted = true;
                    break;
            }
        }
        if (help) {
            printHelp();
            return;
        }
        if (zone == null) {
            System.out.print("Fel: --zone är obligatoriskt (SE1|SE2|SE3|SE4)");
            return;
        }

    }
    public static void printHelp() {
        System.out.println(""" 
                ⚡ Electricity Price Optimizer CLI ⚡
                
                Argument:
                --zone SE1|SE2|SE3|SE4  (obligatoriskt) - vilen elpriszon som ska användas
                --date YYYY-MM-DD       (valfritt)      - datum att hämta priser för (standard = idag)
                --charging 2h|4h|8h     (valfritt)      - laddningsfönster (standard 2h)
                --sorted                (valfritt)      - visa priser i fallande ordning
                --help                  (valfritt)      - visa denna hjälptext
                """);
    }
}



