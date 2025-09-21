package com.example;

import com.example.api.ElpriserAPI;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {


        //Läser in zon
        String zone = System.console().readLine("What zone are you in? (SE1, SE2, SE3 SE4): ");
        zone = zone.toUpperCase();

        if  (zone.equals("SE1") || zone.equals("SE2") || zone.equals("SE3") || zone.equals("SE4")) {
            System.out.println("Zone: " + zone);
        } else {
            System.out.println("Invalid zone");
            System.out.println("Available Zones: SE1, SE2, SE3 SE4");
            System.exit(1);
        }
        ElpriserAPI.Prisklass selectedZone = ElpriserAPI.Prisklass.valueOf(zone);

        //Läser in laddtid
        String inputHours = System.console().readLine("Please enter the charging time: (2/4/8) hours: ");
        int chargingTime;

        try {
            chargingTime = Integer.parseInt(inputHours);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input, please enter 2, 4 or 8.");
            return;
        }

        if (chargingTime == 2 || chargingTime == 4 || chargingTime == 8) {
            System.out.println("Time: " + chargingTime + "h");
        } else {
            System.out.println("Invalid charging time");
            System.out.println("Available Times: 2h/4h/8h");
            System.exit(1);
        }
        //Läser in start + slutdatum
        System.out.println("Please enter the end date (YYYY-MM-DD)");
        System.out.println("Or press enter for 24h ahead");
        String input = System.console().readLine(": ");
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();

        if (input.isEmpty()) {
            endDate = startDate.plusDays(1);
        } else {
            endDate = LocalDate.parse(input);

            if (endDate.isBefore(startDate)) {
                System.out.println("Invalid end date!");
                System.exit(1);
            }
        }


        double minCost = Double.MAX_VALUE;
        double maxCost = Double.MIN_VALUE;
        ZonedDateTime minStart = null;
        ZonedDateTime minEnd = null;
        ZonedDateTime maxStart = null;
        ZonedDateTime maxEnd = null;

        ElpriserAPI api = new ElpriserAPI();

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            List<ElpriserAPI.Elpris> dailyPrices = api.getPriser(d, selectedZone);
            if (dailyPrices.isEmpty()) {
                continue;
            }
            dailyPrices.sort((a, b) -> a.timeStart().compareTo(b.timeStart()));

            for (int i = 0; i <= dailyPrices.size() - chargingTime; i++) {
                double sum = 0;
                for (int j = 0; j < chargingTime; j++) {
                    sum += dailyPrices.get(i + j).sekPerKWh();
                }

                if (sum < minCost) {
                    minCost = sum;
                    minStart = dailyPrices.get(i).timeStart();
                    minEnd = dailyPrices.get(i + chargingTime - 1).timeEnd();
                }
                if (sum > maxCost) {
                    maxCost = sum;
                    maxStart = dailyPrices.get(i).timeStart();
                    maxEnd = dailyPrices.get(i + chargingTime - 1).timeEnd();
                }
            }
        }

        if (minStart != null && maxStart != null) {
            System.out.println("Charging time: " + chargingTime + "h");
            System.out.println("Minimum cost: " + minCost + "h, between " + minStart.toLocalTime() + " and " + minEnd.toLocalTime());
            System.out.println("Maximum cost: " + maxCost + "h, between " + maxStart.toLocalTime() + " and " + maxEnd.toLocalTime());

        } else {
            System.out.println("No prices available");
        }

    }
}
