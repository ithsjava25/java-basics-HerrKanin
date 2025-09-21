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
        LocalDate endDate = LocalDate.now();
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
                            System.exit(1);
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
                            System.exit(1);
                        }
                    }
                    break;
                case "--sorterad":
                    sorted = true;
                    break;
            }
        }
        // Om Standardzon argument saknas
        if (zone == null || !(zone.equals("SE1") || zone.equals("SE2") || zone.equals("SE3") || zone.equals("SE4"))) {
            System.out.println("Error: --zone argument is required! Visar priser för SE1");
            zone = "SE1";
        }

        //Om det inte finns något --date argument
        if (endDate == null) {
            System.out.println("Error: --date argument missing, visar priser 24h framåt ");
            endDate = startDate.plusDays(1);
        }

        //Om slut datum är inna start datum
        if (endDate.isBefore(startDate)) {
            System.out.println("Slut datum kan inte vara innan start datum. Visar priser 24h farmåt");
            endDate = startDate.plusDays(1);
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
            System.exit(1);
        }

        //Sorterar utskrift endast om --sorterad används
        if (sorted) {
            NumberFormat numberFormat = NumberFormat.getInstance(new Locale("sv", "SE"));
            for (ElpriserAPI.Elpris p : allPrices) {
                System.out.println(
                        String.format("%02d-%02d %s öre",
                                p.timeStart().getHour(),
                                p.timeEnd().getHour(),
                                numberFormat.format(p.sekPerKWh())));
            }
        }

        //Beräkna medelpris
        double sumPrice = 0;
        for (ElpriserAPI.Elpris p : allPrices) sumPrice += p.sekPerKWh();
        double meanPrice = sumPrice / allPrices.size();

        if (chargingTime > 0) {
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
            System.out.println("Laddnings tid: " + chargingTime + "h");
            System.out.println("Medelpris: " + numberFormat.format(meanPrice) + " öre");
            System.out.println("Lägsta pris: " + numberFormat.format(minPrice) + " öre. " +
                    "Mellan: " + String.format("%02d", minStart.getHour()) + "-" + String.format("%02d", minEnd.getHour()));
            System.out.println("Högsta pris: " + numberFormat.format(maxPrice) + " öre. " +
                    "Mellan: " + String.format("%02d", maxStart.getHour()) + "-" + String.format("%02d", maxEnd.getHour()));
        }
    }
}




