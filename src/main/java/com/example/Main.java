package com.example;


import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        Locale svenska = new Locale("sv", "SE");
        String zone = null;
        LocalDate date = LocalDate.now();
        String charging = "2h";
        boolean help = false;
        boolean sorted = false;

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
                        System.out.println("Invalid-date: datum formatet måte vara (YYYY-MM-DD)");
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
        List<Double> prices = getPrices(zone, date);

        if (prices.isEmpty()) {
            System.out.println("Fel: Inga priser hittades för " + date);
            return;
        }
        if (sorted) {
            List<Integer> hours = new ArrayList<>();
            List<Double> priceList = new ArrayList<>(prices);

            for (int i = 0; i < priceList.size(); i++) {
                hours.add(i);
            }
            for (int i = 0; i < priceList.size() - 1; i++) {
                    int minIndex = i;
                    for (int j = i + 1; j < priceList.size(); j++) {
                        if (priceList.get(j) < priceList.get(minIndex)) {
                            minIndex = j;
                        }
                    }
                    if (minIndex != i){
                        double tempPrice = priceList.get(i);
                        priceList.set(i, priceList.get(minIndex));
                        priceList.set(minIndex, tempPrice);

                        int tempHour = hours.get(i);
                        hours.set(i, hours.get(minIndex));
                        hours.set(minIndex, tempHour);
                    }
            }
            System.out.println("Priser i stigande ordning:");
            for (int i = 0; i < priceList.size(); i++) {
                System.out.printf(svenska, "%02d-%02d %.2f öre%n",  hours.get(i), hours.get(i) + 1, priceList.get(i) * 100);
            }
            return;
        }

        int hours = switch (charging) {
            case "2h" -> 2;
            case "4h" -> 4;
            case "8h" -> 8;
            default -> 2;
        };
        
        //Bästa laddningsfönster
        if (hours < prices.size()) {
            int bestStart = 0;
            double bestSum = Double.MAX_VALUE;

            for (int i = 0; i <= prices.size() - hours; i++) {
                double sum = 0;
                for (int j = i; j < i + hours; j++) {
                    sum += prices.get(j);
                }
                if (sum < bestSum) {
                    bestSum = sum;
                    bestStart = i;
                }
            }
            double meanWindowPrice = bestSum / hours;

            System.out.printf("Optimalt laddningsfönster (%dh)%n", hours);
            System.out.printf(svenska, "Påbörja laddning kl %02d:00, slut %02d:00, total kostnad %.2f öre%n",
                    bestStart, bestStart + hours, bestSum * 100);
            System.out.printf(svenska, "Medelpris för fönster: %.2f öre/kWh%n", meanWindowPrice * 100);

        }

        // Billigaste och dyraste timme
        int minHourPrice = 0;
        int maxHourPrice = 0;
        for (int i = 0; i < prices.size(); i++) {
            if (prices.get(i) < prices.get(minHourPrice)) minHourPrice = i;
            if (prices.get(i) > prices.get(maxHourPrice)) maxHourPrice = i;
        }
        System.out.printf(svenska, "Lägsta pris timme: %02d-%02d (%.2f öre/kWh)%n", minHourPrice, minHourPrice + 1, prices.get(minHourPrice) * 100);
        System.out.printf(svenska, "Högsta pris timme: %02d-%02d (%.2f öre/kWh)%n", maxHourPrice, maxHourPrice + 1, prices.get(maxHourPrice) * 100);

        // Medelpris
        double meanPrice = 0;
        for (Double price : prices) {
            meanPrice += price;
        }
        System.out.printf(svenska, "Medelpris: %.2f öre/kWh%n", (meanPrice / prices.size()) * 100);

    }

    // Hämtar priser
    private static List<Double> getPrices(String zone, LocalDate date) {
        ElpriserAPI api = new ElpriserAPI();
        ElpriserAPI.Prisklass prisklass;
        try {
            prisklass = ElpriserAPI.Prisklass.valueOf(zone.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Fel: ogiltig zon " + zone + ". Använd (SE1|SE2|SE3|SE4)");
            return Collections.emptyList();
        }
        List<ElpriserAPI.Elpris> elpriser = api.getPriser(date, prisklass);
        List<Double> prices = new ArrayList<>();
        for (ElpriserAPI.Elpris pris : elpriser) {
            prices.add(pris.sekPerKWh());
        }
        return prices;
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
    private record HourPrice (int hour, double price){}
}



