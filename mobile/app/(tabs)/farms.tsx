import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { farmApi } from '../../src/services/api';

export default function FarmsScreen() {
  const router = useRouter();
  const [farms, setFarms] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchFarms = async () => {
    try {
      const res = await farmApi.list();
      setFarms(res.data?.data || []);
    } catch (e) { console.log('Error fetching farms:', e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchFarms(); }, []);
  const onRefresh = () => { setRefreshing(true); fetchFarms().then(() => setRefreshing(false)); };

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]} edges={['top']}>
        <ActivityIndicator size="large" color={colors.primary} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>
        <View style={s.header}>
          <View>
            <Text style={s.title}>My Farms</Text>
            <Text style={s.subtitle}>{farms.length} {farms.length === 1 ? 'farm' : 'farms'} managed</Text>
          </View>
          <TouchableOpacity style={s.addBtn} activeOpacity={0.8} onPress={() => router.push('/add-farm')}>
            <Ionicons name="add" size={20} color={colors.white} />
            <Text style={s.addText}>Add Farm</Text>
          </TouchableOpacity>
        </View>

        {farms.map((farm: any) => {
          const usedPct = farm.totalArea > 0 ? Math.round(((farm.allocatedArea || 0) / farm.totalArea) * 100) : 0;
          const freeArea = Math.max(0, (farm.totalArea || 0) - (farm.allocatedArea || 0));

          return (
            <TouchableOpacity key={farm.id} style={s.farmCard} activeOpacity={0.7}
              onPress={() => router.push({ pathname: '/farm-detail', params: { id: farm.id } })}>
              <View style={s.farmTop}>
                <View style={s.farmIcon}><Ionicons name="business" size={24} color={colors.primary} /></View>
                <View style={s.farmInfo}>
                  <Text style={s.farmName}>{farm.farmName}</Text>
                  <View style={s.farmMeta}>
                    <Ionicons name="location-outline" size={12} color={colors.textMuted} />
                    <Text style={s.farmMetaText}>{farm.village}{farm.district ? `, ${farm.district}` : ''}</Text>
                  </View>
                </View>
                <View style={s.cropBadge}>
                  <Ionicons name="leaf" size={10} color={colors.primary} />
                  <Text style={s.cropBadgeText}>{farm.cropCount || 0} crops</Text>
                </View>
              </View>

              {/* Info row */}
              <View style={s.infoRow}>
                <View style={s.infoItem}>
                  <Ionicons name="resize-outline" size={14} color={colors.textMuted} />
                  <Text style={s.infoText}>{farm.totalArea} {farm.areaUnit || 'acres'}</Text>
                </View>
                <View style={s.infoItem}>
                  <Ionicons name="layers-outline" size={14} color={colors.textMuted} />
                  <Text style={s.infoText}>{farm.soilType || 'Not set'}</Text>
                </View>
                <View style={s.infoItem}>
                  <Ionicons name="map-outline" size={14} color={colors.textMuted} />
                  <Text style={s.infoText}>{farm.state || 'N/A'}</Text>
                </View>
              </View>

              {/* Area bar */}
              <View style={s.barWrap}>
                <View style={s.barBg}>
                  <View style={[s.barFill, { width: `${Math.min(usedPct, 100)}%` }]} />
                </View>
                <View style={s.barLabels}>
                  <Text style={s.barUsed}>{usedPct}% used</Text>
                  <Text style={[s.barFree, freeArea > 0 && { color: colors.primary }]}>{freeArea} {farm.areaUnit || 'acres'} free</Text>
                </View>
              </View>

              {/* Tap hint */}
              <View style={s.tapHint}>
                <Text style={s.tapHintText}>Tap to view details</Text>
                <Ionicons name="chevron-forward" size={14} color={colors.textMuted} />
              </View>
            </TouchableOpacity>
          );
        })}

        {farms.length === 0 && (
          <View style={s.empty}>
            <View style={s.emptyIconBox}>
              <Ionicons name="business-outline" size={48} color={colors.border} />
            </View>
            <Text style={s.emptyTitle}>No Farms Yet</Text>
            <Text style={s.emptySub}>Create your first farm to start managing crops, track growth, and get AI-powered advisories.</Text>
            <TouchableOpacity style={s.emptyBtn} activeOpacity={0.8} onPress={() => router.push('/add-farm')}>
              <Ionicons name="add" size={20} color={colors.white} />
              <Text style={s.emptyBtnText}>Create Farm</Text>
            </TouchableOpacity>
          </View>
        )}

        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: spacing.lg, marginBottom: spacing.xxl },
  title: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text },
  subtitle: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  addBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, backgroundColor: colors.primary, paddingHorizontal: spacing.lg, paddingVertical: spacing.sm + 2, borderRadius: radius.full, ...shadow.sm },
  addText: { fontSize: fontSize.sm, fontWeight: '700', color: colors.white },
  farmCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, marginBottom: spacing.md, ...shadow.sm },
  farmTop: { flexDirection: 'row', alignItems: 'center' },
  farmIcon: { width: 48, height: 48, borderRadius: radius.md, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center', marginRight: spacing.md },
  farmInfo: { flex: 1 },
  farmName: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  farmMeta: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 4 },
  farmMetaText: { fontSize: fontSize.xs, color: colors.textMuted },
  cropBadge: { flexDirection: 'row', alignItems: 'center', gap: 3, backgroundColor: colors.primaryLight, paddingHorizontal: 10, paddingVertical: 4, borderRadius: radius.full },
  cropBadgeText: { fontSize: fontSize.xs, fontWeight: '600', color: colors.primary },
  infoRow: { flexDirection: 'row', gap: spacing.lg, marginTop: spacing.md, paddingTop: spacing.md, borderTopWidth: 1, borderTopColor: colors.borderLight },
  infoItem: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  infoText: { fontSize: fontSize.xs, color: colors.textSecondary },
  barWrap: { marginTop: spacing.md },
  barBg: { height: 6, borderRadius: 3, backgroundColor: colors.borderLight, overflow: 'hidden' },
  barFill: { height: 6, borderRadius: 3, backgroundColor: colors.primary },
  barLabels: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 4 },
  barUsed: { fontSize: fontSize.xs, color: colors.textMuted },
  barFree: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: '600' },
  tapHint: { flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 2, marginTop: spacing.sm },
  tapHintText: { fontSize: 10, color: colors.textMuted },
  empty: { alignItems: 'center', marginTop: 40, backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xxxl, ...shadow.sm },
  emptyIconBox: { width: 88, height: 88, borderRadius: 44, backgroundColor: colors.borderLight, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.lg },
  emptyTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text },
  emptySub: { fontSize: fontSize.md, color: colors.textMuted, marginTop: spacing.xs, textAlign: 'center', lineHeight: 20 },
  emptyBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, backgroundColor: colors.primary, paddingHorizontal: spacing.xxl, paddingVertical: spacing.md, borderRadius: radius.lg, marginTop: spacing.xl },
  emptyBtnText: { fontSize: fontSize.md, fontWeight: '700', color: colors.white },
});
