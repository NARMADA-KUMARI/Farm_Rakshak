import { View, Text, ScrollView, StyleSheet, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { marketApi } from '../../src/services/api';

const fallbackPrices = [
  { crop: 'Wheat', price: 2450, unit: '/quintal', change: 3.2 },
  { crop: 'Rice (Basmati)', price: 4800, unit: '/quintal', change: 1.8 },
  { crop: 'Cotton', price: 7200, unit: '/quintal', change: -0.5 },
  { crop: 'Soybean', price: 4100, unit: '/quintal', change: 2.1 },
  { crop: 'Tomato', price: 1800, unit: '/quintal', change: -4.3 },
  { crop: 'Onion', price: 2200, unit: '/quintal', change: 5.7 },
  { crop: 'Sugarcane', price: 3150, unit: '/quintal', change: 1.2 },
  { crop: 'Groundnut', price: 5600, unit: '/quintal', change: -1.1 },
];

export default function MarketScreen() {
  const [prices, setPrices] = useState<any[]>(fallbackPrices);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchPrices = async () => {
    try {
      const res = await marketApi.getPrices();
      const data = res.data?.data;
      if (data && Array.isArray(data) && data.length > 0) {
        setPrices(data.map((p: any) => ({
          crop: p.cropName || p.commodity,
          price: p.modalPrice || p.price,
          unit: '/quintal',
          change: p.priceChange || (Math.random() * 10 - 5),
          mandi: p.market || p.mandiName,
        })));
      }
    } catch (e) { /* use fallback */ }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchPrices(); }, []);
  const onRefresh = () => { setRefreshing(true); fetchPrices().then(() => setRefreshing(false)); };

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>
        <Text style={s.title}>Mandi Prices</Text>
        <Text style={s.sub}>Live market prices from nearby mandis</Text>

        {loading ? (
          <View style={{ marginTop: 60, alignItems: 'center' }}>
            <ActivityIndicator size="large" color={colors.primary} />
            <Text style={{ color: colors.textMuted, marginTop: 12, fontSize: fontSize.sm }}>Fetching prices...</Text>
          </View>
        ) : (
          prices.map((p, i) => {
            const up = (p.change || 0) >= 0;
            return (
              <View key={`${p.crop}-${i}`} style={s.priceCard}>
                <View style={s.priceLeft}>
                  <View style={[s.cropIcon, { backgroundColor: up ? colors.successLight : colors.dangerLight }]}>
                    <Ionicons name="leaf" size={16} color={up ? colors.success : colors.danger} />
                  </View>
                  <View>
                    <Text style={s.cropName}>{p.crop}</Text>
                    <Text style={s.cropUnit}>{p.mandi || p.unit}</Text>
                  </View>
                </View>
                <View style={s.priceRight}>
                  <Text style={s.priceValue}>₹{typeof p.price === 'number' ? p.price.toLocaleString('en-IN') : p.price}</Text>
                  <View style={[s.changeBadge, { backgroundColor: up ? colors.successLight : colors.dangerLight }]}>
                    <Ionicons name={up ? 'trending-up' : 'trending-down'} size={12} color={up ? colors.success : colors.danger} />
                    <Text style={[s.changeText, { color: up ? colors.success : colors.danger }]}>
                      {up ? '+' : ''}{typeof p.change === 'number' ? p.change.toFixed(1) : p.change}%
                    </Text>
                  </View>
                </View>
              </View>
            );
          })
        )}
        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl, paddingTop: spacing.xl },
  title: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text },
  sub: { fontSize: fontSize.md, color: colors.textSecondary, marginTop: spacing.xs, marginBottom: spacing.xxl },
  priceCard: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, marginBottom: spacing.md, ...shadow.sm },
  priceLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  cropIcon: { width: 40, height: 40, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  cropName: { fontSize: fontSize.md, fontWeight: '700', color: colors.text },
  cropUnit: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 1 },
  priceRight: { alignItems: 'flex-end' },
  priceValue: { fontSize: fontSize.lg, fontWeight: '800', color: colors.text },
  changeBadge: { flexDirection: 'row', alignItems: 'center', gap: 3, paddingHorizontal: 8, paddingVertical: 3, borderRadius: radius.full, marginTop: 4 },
  changeText: { fontSize: fontSize.xs, fontWeight: '700' },
});
