package com.example;

import com.example.api.ElpriserAPI;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        String zone = null;
        int chargingTime = 0;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();
        boolean sorted = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    if (i + 1 < args.length) {
                        zone = args[++i].toLowerCase();
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        try {
                            endDate = LocalDate.parse(args[++i]);
                        } catch (Exception e) {
                            System.out.println("Fel datum format! Använd (yyyy-MM-dd");
                            System.exit(1);
                        }
                    }
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        String value = args[++i].toLowerCase().replace("h", "");
                        try {
                            chargingTime = Integer.parseInt(value);
                            if (chargingTime != 2 && chargingTime != 3 && chargingTime != 8)
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

        if (zone == null) {
            System.out.println("Error: --zone är obligatorisk");
            System.exit(1);
        }
        if (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
            System.out.println("Ogiltig zone! Använd: SE1, SE2, SE3, SE4");
            System.exit(1);
        }

        ElpriserAPI.Prisklass selectedZone = ElpriserAPI.Prisklass.valueOf(zone);

        if (endDate.isBefore(startDate)) {
            System.out.println("Slut datumet kan inte vara innan start datum. Visar 24h farmåt");
            endDate = startDate.plusDays(1);
        }

        ElpriserAPI api = new ElpriserAPI();
        List<ElpriserAPI.Elpris> allPrices = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            List<ElpriserAPI.Elpris> dagspris = api.getPriser(d, selectedZone);
            allPrices.addAll(dagspris);
        }
        if (allPrices.isEmpty()) {
            System.out.println("Inga priser tillgängliga");
            System.exit(1);
        }

        allPrices.sort((a, b) -> a.timeStart().compareTo(b.timeStart()));

        double sumPrice = 0;
        for (ElpriserAPI.Elpris p : allPrices) sumPrice += p.sekPerKWh();
        double meanPrice = sumPrice / allPrices.size();
        System.out.println("Medelvärdet: " + meanPrice);

        if (chargingTime > 0) {
            double minCost = Double.MAX_VALUE;
            double maxCost =  Double.MIN_VALUE;
            ZonedDateTime minStart = null;
            ZonedDateTime minEnd = null;
            ZonedDateTime maxStart = null;
            ZonedDateTime maxEnd = null;

            for (int i = 0; i < allPrices.size() - chargingTime; i++) {
                double windowSum = 0;
                for (int j = 0; j < chargingTime; j++) {
                    windowSum += allPrices.get(i + j).sekPerKWh();
                }
                if (windowSum < minCost) {
                    minCost = windowSum;
                    minStart = allPrices.get(i).timeStart();
                    minEnd = allPrices.get(i + chargingTime - 1).timeEnd();
                }
                if (windowSum > maxCost) {
                    maxCost = windowSum;
                    maxStart = allPrices.get(i).timeStart();
                    maxEnd = allPrices.get(i + chargingTime - 1).timeEnd();
                }
                System.out.println("Laddtid: " + chargingTime + "h");
                System.out.println("Lägst kostnad: " + minCost + " Mellan: " + minStart.toLocalTime() + " - " + minEnd.toLocalTime());
                System.out.println("Högt kostnad: " + maxCost + " Mellan: " + maxStart.toLocalTime() + " - " + maxEnd.toLocalTime());
            }
        }





    }
}




