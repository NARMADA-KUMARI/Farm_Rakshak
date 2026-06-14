import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { advisoryApi } from '../../src/services/api';

const riskColors: Record<string, { badge: string; badgeBg: string; border: string }> = {
  HIGH:   { badge: '#dc2626', badgeBg: '#fef2f2', border: '#fca5a5' },
  MEDIUM: { badge: '#d97706', badgeBg: '#fffbeb', border: '#fcd34d' },
  LOW:    { badge: '#16a34a', badgeBg: '#f0fdf4', border: '#86efac' },
};

const catIcons: Record<string, { icon: string; color: string; bg: string }> = {
  IRRIGATION:   { icon: 'water-outline',     color: '#2563eb', bg: '#eff6ff' },
  FERTILIZER:   { icon: 'flask-outline',     color: '#059669', bg: '#ecfdf5' },
  DISEASE_RISK: { icon: 'shield-outline',    color: '#dc2626', bg: '#fef2f2' },
  PEST_RISK:    { icon: 'bug-outline',       color: '#ea580c', bg: '#fff7ed' },
  WEATHER:      { icon: 'rainy-outline',     color: '#0891b2', bg: '#ecfeff' },
  GENERAL:      { icon: 'bulb-outline',      color: '#d97706', bg: '#fffbeb' },
};

export default function AdvisoryScreen() {
  const router = useRouter();
  const [advisory, setAdvisory] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchAdvisory = async () => {
    try {
      // Use default coords; backend handles location
      const res = await advisoryApi.getAiAdvisory(19.076, 72.8777);
      setAdvisory(res.data?.data || res.data);
    } catch (e) { console.log('Advisory error:', e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchAdvisory(); }, []);
  const onRefresh = () => { setRefreshing(true); fetchAdvisory().then(() => setRefreshing(false)); };

  const crops = advisory?.crops || [];

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>

        {/* Header */}
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Ionicons name="arrow-back" size={22} color={colors.text} />
          </TouchableOpacity>
          <View style={{ flex: 1 }}>
            <Text style={s.headerTitle}>AI Advisory</Text>
            <Text style={s.headerSub}>Smart farming recommendations</Text>
          </View>
          {advisory?.source && (
            <View style={s.sourceBadge}>
              <Ionicons name="sparkles" size={12} color={colors.purple} />
              <Text style={s.sourceText}>
                {advisory.source === 'MERGED' ? 'AI+Rules' : advisory.source === 'AI' ? 'AI Generated' : 'Rule Based'}
              </Text>
            </View>
          )}
        </View>

        {/* Weather Context */}
        {advisory?.weather && (
          <View style={s.weatherBar}>
            <Text style={s.weatherLabel}>Weather Context:</Text>
            <View style={s.weatherItems}>
              <View style={s.weatherItem}>
                <Ionicons name="thermometer-outline" size={14} color="#ef4444" />
                <Text style={s.weatherText}>{advisory.weather.temperature}°C</Text>
              </View>
              <View style={s.weatherItem}>
                <Ionicons name="water-outline" size={14} color="#3b82f6" />
                <Text style={s.weatherText}>{advisory.weather.humidity}%</Text>
              </View>
              <View style={s.weatherItem}>
                <Ionicons name="speedometer-outline" size={14} color="#6b7280" />
                <Text style={s.weatherText}>{advisory.weather.windSpeed} km/h</Text>
              </View>
            </View>
          </View>
        )}

        {loading ? (
          <View style={s.loadingWrap}>
            <ActivityIndicator size="large" color={colors.primary} />
            <Text style={s.loadingText}>Generating AI advisory...</Text>
            <Text style={s.loadingSub}>Analyzing your crops, weather, and soil data</Text>
          </View>
        ) : crops.length === 0 ? (
          <View style={s.emptyState}>
            <Ionicons name="business-outline" size={56} color={colors.border} />
            <Text style={s.emptyTitle}>No farms or crops found</Text>
            <Text style={s.emptySub}>Create a farm and add crops to get AI-powered advisories.</Text>
            <TouchableOpacity style={s.emptyBtn} onPress={() => router.push('/add-farm')}>
              <Ionicons name="add" size={18} color={colors.white} />
              <Text style={s.emptyBtnText}>Create Farm</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={s.cropList}>
            {crops.map((crop: any, idx: number) => {
              const risk = riskColors[crop.overallRisk] || riskColors.LOW;
              return (
                <View key={crop.cropId || idx} style={s.cropCard}>
                  {/* Crop Header */}
                  <View style={s.cropHeader}>
                    <View style={s.cropInfo}>
                      <View style={s.cropIconBox}>
                        <Ionicons name="leaf" size={20} color={colors.primary} />
                      </View>
                      <View style={{ flex: 1 }}>
                        <Text style={s.cropName}>{crop.cropName}</Text>
                        <View style={s.cropMeta}>
                          {crop.farmName && (
                            <View style={s.farmBadge}>
                              <Ionicons name="business" size={10} color={colors.primary} />
                              <Text style={s.farmBadgeText}>{crop.farmName}</Text>
                            </View>
                          )}
                          {crop.cropStage && (
                            <View style={s.stageBadge}>
                              <Text style={s.stageText}>{crop.cropStage}</Text>
                            </View>
                          )}
                        </View>
                      </View>
                    </View>
                    <View style={[s.riskBadge, { backgroundColor: risk.badgeBg, borderColor: risk.border }]}>
                      <Ionicons name="shield" size={14} color={risk.badge} />
                      <Text style={[s.riskText, { color: risk.badge }]}>{crop.overallRisk}</Text>
                    </View>
                  </View>

                  {/* Days since sowing */}
                  {crop.daysSinceSowing > 0 && (
                    <Text style={s.sowingDays}>{crop.daysSinceSowing} days since sowing</Text>
                  )}

                  {/* Suggestions */}
                  <View style={s.suggestionsGrid}>
                    {(crop.suggestions || []).map((sug: any, i: number) => {
                      const cat = catIcons[sug.category] || catIcons.GENERAL;
                      const sRisk = riskColors[sug.riskLevel] || riskColors.LOW;
                      return (
                        <View key={i} style={[s.sugCard, { borderLeftColor: sRisk.border }]}>
                          <View style={s.sugHeader}>
                            <View style={[s.sugIcon, { backgroundColor: cat.bg }]}>
                              <Ionicons name={cat.icon as any} size={14} color={cat.color} />
                            </View>
                            <Text style={s.sugCategory}>
                              {sug.category?.replace('_', ' ')}
                            </Text>
                            <View style={[s.sugRisk, { backgroundColor: sRisk.badgeBg }]}>
                              <Text style={[s.sugRiskText, { color: sRisk.badge }]}>{sug.riskLevel}</Text>
                            </View>
                          </View>
                          <Text style={s.sugMessage}>{sug.message}</Text>
                        </View>
                      );
                    })}
                  </View>
                </View>
              );
            })}
          </View>
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl },
  header: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginTop: spacing.md, marginBottom: spacing.xl },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  headerTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text },
  headerSub: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  sourceBadge: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: colors.purpleLight, paddingHorizontal: spacing.md, paddingVertical: spacing.xs, borderRadius: radius.full },
  sourceText: { fontSize: fontSize.xs, fontWeight: '600', color: colors.purple },
  weatherBar: { backgroundColor: '#eff6ff', borderRadius: radius.lg, padding: spacing.lg, marginBottom: spacing.xl, borderWidth: 1, borderColor: '#bfdbfe' },
  weatherLabel: { fontSize: fontSize.sm, fontWeight: '600', color: '#1e40af', marginBottom: spacing.sm },
  weatherItems: { flexDirection: 'row', gap: spacing.xl },
  weatherItem: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  weatherText: { fontSize: fontSize.sm, color: '#1e40af' },
  loadingWrap: { alignItems: 'center', paddingVertical: 80 },
  loadingText: { color: colors.textMuted, marginTop: 16, fontSize: fontSize.base, fontWeight: '600' },
  loadingSub: { color: colors.textMuted, marginTop: 4, fontSize: fontSize.xs },
  emptyState: { alignItems: 'center', paddingVertical: 60 },
  emptyTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text, marginTop: spacing.lg },
  emptySub: { fontSize: fontSize.md, color: colors.textMuted, textAlign: 'center', marginTop: spacing.sm, paddingHorizontal: spacing.xxl },
  emptyBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, backgroundColor: colors.primary, paddingHorizontal: spacing.xxl, paddingVertical: spacing.md, borderRadius: radius.lg, marginTop: spacing.xl },
  emptyBtnText: { fontSize: fontSize.md, fontWeight: '700', color: colors.white },
  cropList: { gap: spacing.lg },
  cropCard: { backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xl, ...shadow.sm },
  cropHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cropInfo: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, flex: 1 },
  cropIconBox: { width: 44, height: 44, borderRadius: radius.md, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  cropName: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text },
  cropMeta: { flexDirection: 'row', gap: spacing.sm, marginTop: 4, flexWrap: 'wrap' },
  farmBadge: { flexDirection: 'row', alignItems: 'center', gap: 3, backgroundColor: colors.primaryLight, paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.full },
  farmBadgeText: { fontSize: 10, fontWeight: '600', color: colors.primary },
  stageBadge: { backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 2, borderRadius: radius.full },
  stageText: { fontSize: 10, fontWeight: '600', color: '#15803d' },
  riskBadge: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: spacing.md, paddingVertical: spacing.xs, borderRadius: radius.md, borderWidth: 1 },
  riskText: { fontSize: fontSize.xs, fontWeight: '700' },
  sowingDays: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: spacing.sm, marginLeft: 56 },
  suggestionsGrid: { marginTop: spacing.lg, gap: spacing.sm },
  sugCard: { padding: spacing.md, borderRadius: radius.md, backgroundColor: colors.borderLight, borderLeftWidth: 3 },
  sugHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.xs },
  sugIcon: { width: 28, height: 28, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  sugCategory: { fontSize: fontSize.xs, fontWeight: '600', color: colors.text, flex: 1, textTransform: 'capitalize' },
  sugRisk: { paddingHorizontal: 6, paddingVertical: 2, borderRadius: radius.sm },
  sugRiskText: { fontSize: 9, fontWeight: '800' },
  sugMessage: { fontSize: fontSize.sm, color: colors.textSecondary, lineHeight: 20 },
});
