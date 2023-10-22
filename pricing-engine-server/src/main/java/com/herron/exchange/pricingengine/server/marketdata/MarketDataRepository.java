package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataEntry;
import com.herron.exchange.common.api.common.api.marketdata.MarketDataRequest;
import com.herron.exchange.common.api.common.api.marketdata.StaticKey;

import java.time.LocalDate;
import java.util.*;

public class MarketDataRepository {
    private final StaticKey staticKey;
    private final NavigableMap<LocalDate, NavigableSet<MarketDataEntry>> dateToEntry = new TreeMap<>();

    public MarketDataRepository(StaticKey staticKey) {
        this.staticKey = staticKey;
    }

    public synchronized void addEntry(MarketDataEntry entry) {
        dateToEntry.computeIfAbsent(entry.timeComponentKey().date(), k -> new TreeSet<MarketDataEntry>(Comparator.comparing(e -> e.timeComponentKey().timeOfEvent()))).add(entry);
    }

    public synchronized MarketDataEntry getEntry(MarketDataRequest request) {
        return switch (request.timeFilter()) {
            case LATEST -> checkLastest();
            case MATCH_DATE -> checkMatchDate(request);
            case MATCH_TIME -> checkMatchTime(request);
            case MATCH_OR_FIRST_PRIOR -> checkMatchOrFirstPrior(request);
            case MATCH_DATE_AND_TIME -> checkMatchDateAndTime(request);
        };
    }

    private MarketDataEntry checkLastest() {
        return dateToEntry.isEmpty() ? null : dateToEntry.firstEntry().getValue().first();
    }

    private MarketDataEntry checkMatchDate(MarketDataRequest request) {
        return !dateToEntry.containsKey(request.timeComponentKey().date()) ? null : dateToEntry.get(request.timeComponentKey().date()).first();
    }

    private MarketDataEntry checkMatchTime(MarketDataRequest request) {
        var now = LocalDate.now();
        if (!dateToEntry.containsKey(now)) {
            return null;
        }

        for (var item : dateToEntry.get(now)) {
            if (item.timeComponentKey().time().equals(request.timeComponentKey().time())) {
                return item;
            }
        }

        return null;
    }

    private MarketDataEntry checkMatchOrFirstPrior(MarketDataRequest request) {
        var match = checkMatchDateAndTime(request);
        var requestDate = request.timeComponentKey().date();
        if (match == null) {
            var priorDate = requestDate;
            for (var date : dateToEntry.keySet()) {
                if (dateToEntry.containsKey(LocalDate.now())) {
                    priorDate = date;
                    break;
                }
            }
            if (priorDate == null) {
                return null;
            }

            if (priorDate.equals(requestDate)) {
                for (var entry : dateToEntry.get(priorDate)) {
                    if (entry.timeComponentKey().time().isBefore(request.timeComponentKey().time())) {
                        return entry;
                    }
                }

            } else if (priorDate.isBefore(requestDate)) {
                return dateToEntry.get(priorDate).first();
            }
        }
        return null;
    }

    private MarketDataEntry checkMatchDateAndTime(MarketDataRequest request) {
        var now = LocalDate.now();
        if (!dateToEntry.containsKey(now)) {
            return null;
        }
        for (var item : dateToEntry.get(now)) {
            if (item.timeComponentKey().time().equals(request.timeComponentKey().time())) {
                return item;
            }
        }
        return null;
    }
}
