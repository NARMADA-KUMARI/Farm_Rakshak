import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { farmApi } from '../../src/services/api';

export default function FarmDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [farm, setFarm] = useState<any>(null);
  const [crops, setCrops] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchData = async () => {
    if (!id) return;
    try {
      const [farmRes, cropsRes] = await Promise.allSettled([
        farmApi.getById(id),
        farmApi.listCrops(id),
      ]);
      if (farmRes.status === 'fulfilled') setFarm(farmRes.value.data?.data || farmRes.value.data);
      if (cropsRes.status === 'fulfilled') setCrops(cropsRes.value.data?.data || []);
    } catch (e) { console.log('Farm detail error:', e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, [id]);
  const onRefresh = () => { setRefreshing(true); fetchData().then(() => setRefreshing(false)); };

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </SafeAreaView>
    );
  }

  const usedPct = farm?.totalArea > 0 ? Math.round(((farm.allocatedArea || 0) / farm.totalArea) * 100) : 0;
  const freeArea = Math.max(0, (farm?.totalArea || 0) - (farm?.allocatedArea || 0));

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>

        {/* Header */}
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Ionicons name="arrow-back" size={22} color={colors.text} />
          </TouchableOpacity>
          <Text style={s.headerTitle} numberOfLines={1}>{farm?.farmName || 'Farm Details'}</Text>
          <View style={{ width: 40 }} />
        </View>

        {farm && (
          <>
            {/* Farm Info Card */}
            <View style={s.farmCard}>
              <View style={s.farmIconBox}>
                <Ionicons name="business" size={28} color={colors.primary} />
              </View>
              <Text style={s.farmName}>{farm.farmName}</Text>
              {farm.village && (
                <View style={s.locationRow}>
                  <Ionicons name="location" size={14} color={colors.textMuted} />
                  <Text style={s.locationText}>
                    {farm.village}{farm.district ? `, ${farm.district}` : ''}{farm.state ? `, ${farm.state}` : ''}
                  </Text>
                </View>
              )}

              {/* Stats Row */}
              <View style={s.statsRow}>
                <View style={s.statItem}>
                  <Ionicons name="resize-outline" size={16} color={colors.primary} />
                  <Text style={s.statValue}>{farm.totalArea}</Text>
                  <Text style={s.statLabel}>{farm.areaUnit || 'acres'}</Text>
                </View>
                <View style={s.statDivider} />
                <View style={s.statItem}>
                  <Ionicons name="leaf" size={16} color={colors.accent} />
                  <Text style={s.statValue}>{farm.cropCount || crops.length}</Text>
                  <Text style={s.statLabel}>crops</Text>
                </View>
                <View style={s.statDivider} />
                <View style={s.statItem}>
                  <Ionicons name="layers-outline" size={16} color={colors.info} />
                  <Text style={s.statValue}>{farm.soilType || 'N/A'}</Text>
                  <Text style={s.statLabel}>soil</Text>
                </View>
              </View>

              {/* Area Progress */}
              <View style={s.progressWrap}>
                <View style={s.progressBar}>
                  <View style={[s.progressFill, { width: `${Math.min(usedPct, 100)}%` }]} />
                </View>
                <View style={s.progressLabels}>
                  <Text style={s.progressUsed}>{usedPct}% allocated</Text>
                  <Text style={[s.progressFree, freeArea > 0 && { color: colors.primary }]}>{freeArea} {farm.areaUnit || 'acres'} free</Text>
                </View>
              </View>
            </View>

            {/* Crops Section */}
            <View style={s.sectionHeader}>
              <View style={s.sectionLeft}>
                <View style={s.sectionIcon}>
                  <Ionicons name="leaf" size={16} color={colors.primary} />
                </View>
                <Text style={s.sectionTitle}>Crops ({crops.length})</Text>
              </View>
              <TouchableOpacity style={s.addCropBtn}>
                <Ionicons name="add" size={16} color={colors.primary} />
                <Text style={s.addCropText}>Add Crop</Text>
              </TouchableOpacity>
            </View>

            {crops.length === 0 ? (
              <View style={s.emptyCrops}>
                <Ionicons name="leaf-outline" size={40} color={colors.border} />
                <Text style={s.emptyText}>No crops added yet</Text>
                <Text style={s.emptySub}>Add a crop to start tracking its lifecycle</Text>
              </View>
            ) : (
              crops.map((crop: any) => {
                const daysGrown = crop.daysSinceSowing || 0;
                return (
                  <TouchableOpacity key={crop.id || crop.userCropId} style={s.cropCard} activeOpacity={0.7}
                    onPress={() => router.push({ pathname: '/crop-detail', params: { id: crop.id || crop.userCropId } })}>
                    <View style={s.cropTop}>
                      <View style={s.cropIconBox}>
                        <Ionicons name="leaf" size={18} color={colors.primary} />
                      </View>
                      <View style={{ flex: 1 }}>
                        <Text style={s.cropName}>{crop.cropName}</Text>
                        <Text style={s.cropMeta}>
                          {crop.variety ? `${crop.variety} · ` : ''}Sown: {crop.sowingDate ? new Date(crop.sowingDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : 'N/A'}
                        </Text>
                      </View>
                      <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
                    </View>
                    <View style={s.cropDetails}>
                      {crop.currentStage && (
                        <View style={s.cropTag}>
                          <Text style={s.cropTagText}>{crop.currentStage}</Text>
                        </View>
                      )}
                      {daysGrown > 0 && (
                        <Text style={s.daysText}>Day {daysGrown}</Text>
                      )}
                      {crop.areaAllocated && (
                        <Text style={s.areaText}>{crop.areaAllocated} {farm.areaUnit || 'acres'}</Text>
                      )}
                    </View>
                  </TouchableOpacity>
                );
              })
            )}
          </>
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: spacing.md, marginBottom: spacing.xl },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  headerTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text, flex: 1, textAlign: 'center' },
  farmCard: { backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xxl, alignItems: 'center', marginBottom: spacing.xl, ...shadow.md },
  farmIconBox: { width: 64, height: 64, borderRadius: radius.lg, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.md },
  farmName: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text },
  locationRow: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: spacing.sm },
  locationText: { fontSize: fontSize.sm, color: colors.textMuted },
  statsRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.xl, marginTop: spacing.xl, paddingTop: spacing.xl, borderTopWidth: 1, borderTopColor: colors.borderLight },
  statItem: { alignItems: 'center', gap: 2 },
  statValue: { fontSize: fontSize.lg, fontWeight: '800', color: colors.text },
  statLabel: { fontSize: fontSize.xs, color: colors.textMuted },
  statDivider: { width: 1, height: 32, backgroundColor: colors.border },
  progressWrap: { width: '100%', marginTop: spacing.xl },
  progressBar: { height: 8, borderRadius: 4, backgroundColor: colors.borderLight, overflow: 'hidden' },
  progressFill: { height: 8, borderRadius: 4, backgroundColor: colors.primary },
  progressLabels: { flexDirection: 'row', justifyContent: 'space-between', marginTop: spacing.xs },
  progressUsed: { fontSize: fontSize.xs, color: colors.textMuted },
  progressFree: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: '600' },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.lg },
  sectionLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  sectionIcon: { width: 32, height: 32, borderRadius: radius.sm, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  sectionTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text },
  addCropBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: spacing.md, paddingVertical: spacing.sm, borderRadius: radius.full, borderWidth: 1, borderColor: colors.primary },
  addCropText: { fontSize: fontSize.xs, fontWeight: '600', color: colors.primary },
  emptyCrops: { alignItems: 'center', paddingVertical: spacing.xxxl, backgroundColor: colors.white, borderRadius: radius.lg, ...shadow.sm },
  emptyText: { fontSize: fontSize.base, fontWeight: '600', color: colors.text, marginTop: spacing.md },
  emptySub: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.xs },
  cropCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, marginBottom: spacing.sm, ...shadow.sm },
  cropTop: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  cropIconBox: { width: 40, height: 40, borderRadius: radius.sm, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  cropName: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  cropMeta: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  cropDetails: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.md, paddingTop: spacing.md, borderTopWidth: 1, borderTopColor: colors.borderLight, flexWrap: 'wrap' },
  cropTag: { backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 3, borderRadius: radius.full },
  cropTagText: { fontSize: 10, fontWeight: '600', color: '#15803d' },
  daysText: { fontSize: fontSize.xs, color: colors.textSecondary },
  areaText: { fontSize: fontSize.xs, color: colors.info, fontWeight: '600' },
});
