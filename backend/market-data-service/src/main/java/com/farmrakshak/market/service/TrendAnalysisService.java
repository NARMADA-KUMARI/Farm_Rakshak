package com.farmrakshak.market.service;

import com.farmrakshak.market.dto.TrendResponse;
import com.farmrakshak.market.entity.CropPrice;
import com.farmrakshak.market.repository.CropPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Price trend analysis — moving averages, volatility, direction detection.
 */
@Service
@RequiredArgsConstructor
public class TrendAnalysisService {

    private final CropPriceRepository cropPriceRepository;

    /**
     * Compute trend data for a crop over the given number of days.
     */
    public TrendResponse analyzeTrend(String cropName, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<CropPrice> prices = cropPriceRepository.findTrendData(cropName, startDate, endDate);

        if (prices.isEmpty()) {
            return TrendResponse.builder()
                    .cropName(cropName)
                    .days(days)
                    .trend("STABLE")
                    .dataPoints(List.of())
                    .build();
        }

        // Aggregate by date — average across all mandis
        Map<LocalDate, List<CropPrice>> byDate = prices.stream()
                .collect(Collectors.groupingBy(CropPrice::getPriceDate));

        List<TrendResponse.TrendDataPoint> dataPoints = new ArrayList<>();
        List<BigDecimal> modalPrices = new ArrayList<>();

        byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    BigDecimal avgModal = average(entry.getValue().stream()
                            .map(CropPrice::getPriceModal).toList());
                    BigDecimal avgMin = average(entry.getValue().stream()
                            .map(CropPrice::getPriceMin).toList());
                    BigDecimal avgMax = average(entry.getValue().stream()
                            .map(CropPrice::getPriceMax).toList());
                    BigDecimal avgArrival = average(entry.getValue().stream()
                            .map(CropPrice::getArrivalQuantity).toList());

                    modalPrices.add(avgModal);

                    dataPoints.add(TrendResponse.TrendDataPoint.builder()
                            .date(entry.getKey().toString())
                            .priceModal(avgModal)
                            .priceMin(avgMin)
                            .priceMax(avgMax)
                            .arrivalQuantity(avgArrival)
                            .build());
                });

        // Compute moving averages (3-day window)
        for (int i = 0; i < dataPoints.size(); i++) {
            int start = Math.max(0, i - 2);
            List<BigDecimal> window = modalPrices.subList(start, i + 1);
            dataPoints.get(i).setMovingAvg(average(window));
        }

        // Trend direction
        BigDecimal startPrice = modalPrices.isEmpty() ? BigDecimal.ZERO : modalPrices.get(0);
        BigDecimal endPrice = modalPrices.isEmpty() ? BigDecimal.ZERO : modalPrices.get(modalPrices.size() - 1);
        BigDecimal changePercent = BigDecimal.ZERO;

        if (startPrice.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = endPrice.subtract(startPrice)
                    .divide(startPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        String trend = determineTrend(changePercent);
        BigDecimal movingAvg = average(modalPrices);
        BigDecimal volatility = computeVolatility(modalPrices, movingAvg);

        return TrendResponse.builder()
                .cropName(cropName)
                .days(days)
                .trend(trend)
                .startPrice(startPrice)
                .endPrice(endPrice)
                .changePercent(changePercent)
                .movingAverage(movingAvg)
                .volatility(volatility)
                .dataPoints(dataPoints)
                .build();
    }

    public String determineTrend(BigDecimal changePercent) {
        if (changePercent.compareTo(BigDecimal.valueOf(3)) > 0) return "UP";
        if (changePercent.compareTo(BigDecimal.valueOf(-3)) < 0) return "DOWN";
        return "STABLE";
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream()
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = values.stream().filter(java.util.Objects::nonNull).count();
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal computeVolatility(List<BigDecimal> prices, BigDecimal mean) {
        if (prices.size() < 2 || mean.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        double variance = prices.stream()
                .filter(java.util.Objects::nonNull)
                .mapToDouble(p -> {
                    double diff = p.subtract(mean).doubleValue();
                    return diff * diff;
                })
                .average().orElse(0);

        double stdDev = Math.sqrt(variance);
        double cv = (stdDev / mean.doubleValue()) * 100; // Coefficient of variation
        return BigDecimal.valueOf(cv).setScale(2, RoundingMode.HALF_UP);
    }
}
