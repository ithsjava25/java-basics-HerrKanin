package com.example;

import com.example.api.ElpriserAPI;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        String zone = null;
        int chargingTime = 0;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate;
        boolean sorted = false;

        //Läser in argument
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    if (i + 1 < args.length) {
                        zone = args[++i].toUpperCase();
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        try {
                            endDate = LocalDate.parse(args[++i]);
                        } catch (Exception e) {
                            System.out.println("Fel datum format! Använd (yyyy-MM-dd)");
                        }
                    }
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        String value = args[++i].toLowerCase().replace("h", "");
                        try {
                            chargingTime = Integer.parseInt(value);
                            if (chargingTime != 2 && chargingTime != 4 && chargingTime != 8)
                                throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                            System.out.println("Ogiltig tidintervall! Använd 2h, 4h, eller 8h");
                        }
                    }
                    break;
                case "--sorterad":
                    sorted = true;
                    break;
                case "--help":
                    printHelp();
                    return;
            }
        }
        if (args.length == 0) {
            printHelp();
            System.out.println("Inga argument angivna, använder standardvärden:");
            System.out.println("Zon: SE1, Tid: 2h laddning, Period: 24h framåt");
            zone = "SE1";
            chargingTime = 2;
        }
        // Om Standardzon argument saknas
        if (zone == null) {
            System.out.println("Error: --zone argument is required! Visar priser för SE1");
            zone = "SE1";
        }
        if (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
            System.out.println("Error: invalid zone! Visar priser för SE1");
            zone = "SE1";
        }

        //Om det inte finns något --date argument
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }
        if (chargingTime < 0) {
            System.out.println("Error: chargingTime invalid. Visat för 2h period.");
            chargingTime = 2;
        }

        ElpriserAPI.Prisklass selectedZone = ElpriserAPI.Prisklass.valueOf(zone);

        //Hämta priser
        ElpriserAPI api = new ElpriserAPI();
        List<ElpriserAPI.Elpris> allPrices = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            allPrices.addAll(api.getPriser(d, selectedZone));
        }
        if (allPrices.isEmpty()) {
            System.out.println("Inga priser tillgängliga");
        }
        if (sorted && !allPrices.isEmpty()) {
            NumberFormat numberFormat = NumberFormat.getInstance(new Locale("sv", "SE"));
            for (ElpriserAPI.Elpris p : allPrices) {
                System.out.println(
                        String.format("%02d-%02d %s öre",
                                p.timeStart().getHour(),
                                p.timeEnd().getHour(),
                                numberFormat.format(p.sekPerKWh()))
                );
            }
        }

        //Beräkna medelpris
        double sumPrice = 0;
        for (ElpriserAPI.Elpris p : allPrices) sumPrice += p.sekPerKWh();
        double meanPrice = sumPrice / allPrices.size();


        if (chargingTime > 0) {
            if (allPrices.size() < chargingTime) {
                System.out.println("Inte tillräckligt med priser för laddningsfönster");
            } else {
                double minPrice = Double.MAX_VALUE;
                double maxPrice = Double.MIN_VALUE;
                ZonedDateTime minStart = null;
                ZonedDateTime minEnd = null;
                ZonedDateTime maxStart = null;
                ZonedDateTime maxEnd = null;

                for (int i = 0; i < allPrices.size() - chargingTime; i++) {
                    double windowSum = 0;
                    for (int j = 0; j < chargingTime; j++) {
                        windowSum += allPrices.get(i + j).sekPerKWh();
                    }
                    if (windowSum < minPrice) {
                        minPrice = windowSum;
                        minStart = allPrices.get(i).timeStart();
                        minEnd = allPrices.get(i + chargingTime - 1).timeEnd();
                    }
                    if (windowSum > maxPrice) {
                        maxPrice = windowSum;
                        maxStart = allPrices.get(i).timeStart();
                        maxEnd = allPrices.get(i + chargingTime - 1).timeEnd();
                    }
                }
                NumberFormat numberFormat = NumberFormat.getInstance(new Locale("sv", "SE"));
                double meanWindowPrice = (minPrice / chargingTime) * 100;
                double minPriceore = minPrice * 100;
                double maxPriceore = maxPrice * 100;

                System.out.println("Laddnings tid: " + chargingTime + "h");
                System.out.println("Påbörja laddning kl " + String.format("%02d:00", minStart.getHour()));
                System.out.println("Medelpris för fönster: " + numberFormat.format(meanWindowPrice) + " öre");
                System.out.println("Lägsta pris: " + numberFormat.format(minPriceore) + " öre. " +
                        "Mellan: " + String.format("%02d", minStart.getHour()) + "-" + String.format("%02d", minEnd.getHour()));
                System.out.println("Högsta pris: " + numberFormat.format(maxPriceore) + " öre. " +
                        "Mellan: " + String.format("%02d", maxStart.getHour()) + "-" + String.format("%02d", maxEnd.getHour()));
            }
        }
    }
    public static void printHelp() {
        System.out.println("Usage: java -jar app.jar");
        System.out.println("Options:");
        System.out.println(" --zone (SE1/SE2/SE3/SE4)   Välj område.");
        System.out.println(" --date (yyyy-MM-dd)        Välj datum.");
        System.out.println(" --charging (2h/4h/8h)      Ange laddningstid");
        System.out.println(" --sorted                   Sorterar priser");
        System.out.println(" --help                     Visar hjälp");
    }
}




