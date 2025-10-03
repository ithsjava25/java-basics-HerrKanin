package com.example;


import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        Locale formatToSwedish = new Locale("sv", "SE");
        String zone = null;
        LocalDate date = LocalDate.now();
        String charging = "2h";
        boolean help = false;
        boolean sorted = false;

        // Argumenthantering
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
                    try {
                        if (i + 1 < args.length) {
                            date = LocalDate.parse(args[++i]);
                        }
                    } catch (DateTimeParseException e) {
                        System.out.println("Fel: ogiltigt datum, formatet måste vara (yyyy-MM-dd) ");
                        return;
                    }
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
            }
        }
        if (help || args.length == 0) {
            printHelp();
            return;
        }
        if (zone == null) {
            System.out.print("Fel: --zone är obligatoriskt (SE1|SE2|SE3|SE4)");
            return;
        }
        //Hämtar priser från API
        List<ElpriserAPI.Elpris> pricesToday = getPrices(zone, date);
        List<ElpriserAPI.Elpris> pricesTomorrow = getPrices(zone, date.plusDays(1));

        if (pricesToday.size() > 24) pricesToday = pricesToday.subList(0, 24);
        if (pricesTomorrow.size() > 24) pricesTomorrow = pricesTomorrow.subList(0, 24);

        List<ElpriserAPI.Elpris> allPrices = new ArrayList<>();
        allPrices.addAll(pricesToday);
        allPrices.addAll(pricesTomorrow);

        if (allPrices.isEmpty()) {
            System.out.println("Fel: Inga priser hittades för " + date);
            return;
        }

        // Sorterar priser om --sorted används
        if (sorted) {
            List<Integer> hours = new ArrayList<>();
            List<Double> priceList = new ArrayList<>();

            //Fyller listorna med priser från API och timmar från hours
            for (int i = 0; i < pricesToday.size(); i++) {
                hours.add(i);
                priceList.add(pricesToday.get(i).sekPerKWh());
            }
            // Selection sort
            for (int i = 0; i < priceList.size() - 1 ; i++) {
                int minIndex = i;
                for (int j = i + 1; j < priceList.size(); j++) {
                    if (priceList.get(j) < priceList.get(minIndex)) {
                        minIndex = j;
                    }
                }
                if (minIndex != i) {
                    // Byt plats i prislistan
                    double tempPrice = priceList.get(i);
                    priceList.set(i, priceList.get(minIndex));
                    priceList.set(minIndex, tempPrice);

                    // Byt plats i timlistan så timmen följer priset
                    int tempHour = hours.get(i);
                    hours.set(i, hours.get(minIndex));
                    hours.set(minIndex, tempHour);
                }
            }
            //Skriver ut sorterade priser med timme
            System.out.println("Priser i stigande ordning:");
            for (int i = 0; i < priceList.size(); i++) {
                System.out.printf(formatToSwedish, "%02d-%02d %.2f öre%n", hours.get(i), hours.get(i) + 1, priceList.get(i) * 100);
            }
            return;
        }
        // Bestäm laddningstimmar
        int hours = switch (charging) {
            case "2h" -> 2;
            case "4h" -> 4;
            case "8h" -> 8;
            default -> 2;
        };

        //Hitta bästa laddningsfönster
        if (hours <= allPrices.size()) {
            int bestStart = 0;
            double bestSum = Double.MAX_VALUE;

            for (int i = 0; i <= allPrices.size() - hours; i++) {
                double sum = 0;
                for (int j = i; j < i + hours; j++) {
                    sum += allPrices.get(j).sekPerKWh();
                }
                if (sum < bestSum) {
                    bestSum = sum;
                    bestStart = i;
                }
            }
            double meanWindowPrice = bestSum / hours;
            int startHour = allPrices.get(bestStart).timeStart().getHour();  // Start timme = index modulo 24
            int endHour = allPrices.get(bestStart + hours - 1).timeEnd().getHour(); // Sluttimme

            System.out.printf("Optimalt laddningsfönster (%dh)%n", hours);
            System.out.printf(formatToSwedish, "Påbörja laddning kl %02d:00, slut %02d:00, total kostnad %.2f öre%n",
                    startHour, endHour, bestSum * 100);
            System.out.printf(formatToSwedish, "Medelpris för fönster: %.2f öre/kWh%n", meanWindowPrice * 100);
        }

        // Hitta billigaste och dyraste timme
        int minHour = 0;
        int maxHour = 0;
        for (int i = 0; i < allPrices.size(); i++) {
            if (allPrices.get(i).sekPerKWh() < allPrices.get(minHour).sekPerKWh()) minHour = i;
            if (allPrices.get(i).sekPerKWh() > allPrices.get(maxHour).sekPerKWh()) maxHour = i;
        }

        System.out.printf(formatToSwedish, "Lägsta pris timme: %02d-%02d (%.2f öre/kWh)%n", minHour % 24 , (minHour % 24) + 1, allPrices.get(minHour).sekPerKWh() * 100);
        System.out.printf(formatToSwedish, "Högsta pris timme: %02d-%02d (%.2f öre/kWh)%n", maxHour % 24, (maxHour % 24) + 1, allPrices.get(maxHour).sekPerKWh() * 100);

        // Beräknar medelpris
        double meanPrice = 0;
        for (ElpriserAPI.Elpris price : allPrices) {
            meanPrice += price.sekPerKWh();
        }
        System.out.printf(formatToSwedish, "Medelpris: %.2f öre/kWh%n", (meanPrice / allPrices.size()) * 100);
    }

    // Hämtar priser från API
    private static List<ElpriserAPI.Elpris> getPrices(String zone, LocalDate date) {
        ElpriserAPI api = new ElpriserAPI();
        ElpriserAPI.Prisklass prisklass;
        try {
            prisklass = ElpriserAPI.Prisklass.valueOf(zone.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Fel: ogiltig zon " + zone + ". Använd (SE1|SE2|SE3|SE4)");
            return Collections.emptyList();
        }
        return api.getPriser(date, prisklass);
    }

    // Hjälptext
    private static void printHelp() {
        System.out.println(""" 
                ⚡ Electricity Price Optimizer CLI ⚡
                
                Usage:
                jaca -cp target/classes com.example.Main --zone SE1
                
                Argument:
                --zone SE1|SE2|SE3|SE4  (obligatoriskt) - vilken elpriszon som ska användas
                --date YYYY-MM-DD       (valfritt)      - datum att hämta priser för (standard = idag)
                --charging 2h|4h|8h     (valfritt)      - laddningsfönster (standard 2h)
                --sorted                (valfritt)      - visa priser i fallande ordning
                --help                  (valfritt)      - visa denna hjälptext
                """);
    }

}



