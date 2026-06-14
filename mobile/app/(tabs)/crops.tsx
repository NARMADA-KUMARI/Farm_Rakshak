import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { cropApi } from '../../src/services/api';

export default function CropsScreen() {
  const router = useRouter();
  const [crops, setCrops] = useState<any[]>([]);
  const [todayTasks, setTodayTasks] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchData = async () => {
    try {
      const [cropsRes, tasksRes] = await Promise.allSettled([
        cropApi.getMyCrops(),
        cropApi.getTodaysTasks(),
      ]);
      if (cropsRes.status === 'fulfilled') setCrops(cropsRes.value.data?.data || []);
      if (tasksRes.status === 'fulfilled') setTodayTasks(tasksRes.value.data?.data || []);
    } catch (e) { console.log('Crops error:', e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);
  const onRefresh = () => { setRefreshing(true); fetchData().then(() => setRefreshing(false)); };

  const pendingTasks = todayTasks.filter(t => !t.isCompleted);
  const overdueTasks = todayTasks.filter(t => t.isOverdue);

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]} edges={['top']}>
        <ActivityIndicator size="large" color={colors.primary} />
        <Text style={s.loadingText}>Loading crops...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>

        <View style={s.header}>
          <View>
            <Text style={s.title}>My Crops</Text>
            <Text style={s.subtitle}>{crops.length} crops · {pendingTasks.length} tasks today</Text>
          </View>
        </View>

        {/* Stats Row */}
        <View style={s.statsRow}>
          <View style={s.statCard}>
            <Ionicons name="leaf" size={18} color={colors.primary} />
            <Text style={s.statValue}>{crops.length}</Text>
            <Text style={s.statLabel}>Active Crops</Text>
          </View>
          <View style={[s.statCard, overdueTasks.length > 0 && s.statCardAlert]}>
            <Ionicons name="alert-circle" size={18} color={overdueTasks.length > 0 ? colors.danger : colors.warning} />
            <Text style={s.statValue}>{overdueTasks.length}</Text>
            <Text style={s.statLabel}>Overdue</Text>
          </View>
          <View style={s.statCard}>
            <Ionicons name="checkbox-outline" size={18} color={colors.info} />
            <Text style={s.statValue}>{pendingTasks.length}</Text>
            <Text style={s.statLabel}>Pending</Text>
          </View>
        </View>

        {/* Today's Tasks */}
        {pendingTasks.length > 0 && (
          <>
            <View style={s.sectionHeader}>
              <View style={s.sectionLeft}>
                <View style={[s.sectionIcon, { backgroundColor: colors.warningLight }]}>
                  <Ionicons name="today-outline" size={14} color={colors.warning} />
                </View>
                <Text style={s.sectionTitle}>Today's Tasks</Text>
              </View>
              <Text style={s.taskCount}>{pendingTasks.length} pending</Text>
            </View>
            {pendingTasks.slice(0, 5).map((task: any, i: number) => (
              <TouchableOpacity key={task.id || i} style={[s.taskCard, task.isOverdue && s.taskOverdue]} activeOpacity={0.7}
                onPress={() => cropApi.completeTask(task.id).then(() => fetchData()).catch(() => {})}>
                <Ionicons name="ellipse-outline" size={20} color={task.isOverdue ? colors.danger : colors.textMuted} />
                <View style={{ flex: 1 }}>
                  <Text style={s.taskTitle}>{task.title}</Text>
                  <View style={s.taskMeta}>
                    {task.cropName && <Text style={s.taskCrop}>🌱 {task.cropName}</Text>}
                    {task.stage && <Text style={s.taskStage}>{task.stage}</Text>}
                    <Text style={s.taskDay}>Day {task.dayNumber}</Text>
                    {task.isOverdue && <Text style={s.taskOverdueText}>⚠️ Overdue</Text>}
                  </View>
                </View>
              </TouchableOpacity>
            ))}
          </>
        )}

        {/* Crop List */}
        <View style={[s.sectionHeader, { marginTop: spacing.xl }]}>
          <View style={s.sectionLeft}>
            <View style={[s.sectionIcon, { backgroundColor: colors.primaryLight }]}>
              <Ionicons name="leaf" size={14} color={colors.primary} />
            </View>
            <Text style={s.sectionTitle}>All Crops</Text>
          </View>
        </View>

        {crops.length === 0 ? (
          <View style={s.emptyState}>
            <View style={s.emptyIcon}>
              <Ionicons name="leaf-outline" size={48} color={colors.border} />
            </View>
            <Text style={s.emptyTitle}>No Crops Yet</Text>
            <Text style={s.emptySub}>Go to your farm and add crops to start tracking growth stages and get daily tasks.</Text>
            <TouchableOpacity style={s.emptyBtn} onPress={() => router.push('/(tabs)/farms')} activeOpacity={0.8}>
              <Ionicons name="business-outline" size={18} color={colors.white} />
              <Text style={s.emptyBtnText}>Go to Farms</Text>
            </TouchableOpacity>
          </View>
        ) : (
          crops.map((crop: any) => {
            const daysGrown = crop.daysSinceSowing || 0;
            const progress = crop.totalDays ? Math.min(Math.round((daysGrown / crop.totalDays) * 100), 100) : 0;
            return (
              <TouchableOpacity key={crop.userCropId || crop.id} style={s.cropCard} activeOpacity={0.7}
                onPress={() => router.push({ pathname: '/(tabs)/crop-detail', params: { id: crop.userCropId || crop.id } })}>
                <View style={s.cropTop}>
                  <View style={s.cropIconBox}>
                    <Ionicons name="leaf" size={20} color={colors.primary} />
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={s.cropName}>{crop.cropName}</Text>
                    <Text style={s.cropMeta}>
                      {crop.variety ? `${crop.variety} · ` : ''}{crop.farmName || ''}
                    </Text>
                  </View>
                  <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
                </View>

                {/* Progress + Stage */}
                <View style={s.cropProgress}>
                  <View style={s.cropTags}>
                    {crop.currentStage && (
                      <View style={s.stageTag}>
                        <Text style={s.stageTagText}>📌 {crop.currentStage}</Text>
                      </View>
                    )}
                    {daysGrown > 0 && <Text style={s.daysText}>Day {daysGrown}</Text>}
                  </View>
                  {progress > 0 && (
                    <View style={s.progressWrap}>
                      <View style={s.progressBar}>
                        <View style={[s.progressFill, { width: `${progress}%` }]} />
                      </View>
                      <Text style={s.progressText}>{progress}%</Text>
                    </View>
                  )}
                </View>

                {/* Sowing info */}
                <View style={s.cropBottom}>
                  <View style={s.cropInfoItem}>
                    <Ionicons name="calendar-outline" size={12} color={colors.textMuted} />
                    <Text style={s.cropInfoText}>
                      Sown: {crop.sowingDate ? new Date(crop.sowingDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : 'N/A'}
                    </Text>
                  </View>
                  {crop.areaAllocated && (
                    <View style={s.cropInfoItem}>
                      <Ionicons name="resize-outline" size={12} color={colors.textMuted} />
                      <Text style={s.cropInfoText}>{crop.areaAllocated} acres</Text>
                    </View>
                  )}
                </View>
              </TouchableOpacity>
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
  container: { flex: 1, paddingHorizontal: spacing.xl },
  loadingText: { color: colors.textMuted, marginTop: 12, fontSize: fontSize.sm },
  header: { marginTop: spacing.lg, marginBottom: spacing.xl },
  title: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text },
  subtitle: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  statsRow: { flexDirection: 'row', gap: spacing.md, marginBottom: spacing.xl },
  statCard: { flex: 1, backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.md, alignItems: 'center', ...shadow.sm },
  statCardAlert: { backgroundColor: '#fef2f2', borderWidth: 1, borderColor: '#fecaca' },
  statValue: { fontSize: fontSize.xl, fontWeight: '800', color: colors.text, marginTop: spacing.xs },
  statLabel: { fontSize: 10, color: colors.textMuted, marginTop: 2, fontWeight: '500' },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.md },
  sectionLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  sectionIcon: { width: 28, height: 28, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  sectionTitle: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  taskCount: { fontSize: fontSize.xs, color: colors.warning, fontWeight: '600' },
  taskCard: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md, backgroundColor: '#fffbeb', borderRadius: radius.md, padding: spacing.md, marginBottom: spacing.sm, borderWidth: 1, borderColor: '#fde68a' },
  taskOverdue: { backgroundColor: '#fef2f2', borderColor: '#fecaca' },
  taskTitle: { fontSize: fontSize.sm, fontWeight: '600', color: colors.text },
  taskMeta: { flexDirection: 'row', gap: spacing.sm, marginTop: 3, flexWrap: 'wrap' },
  taskCrop: { fontSize: 10, fontWeight: '600', color: colors.primary },
  taskStage: { fontSize: 10, backgroundColor: 'rgba(255,255,255,0.8)', paddingHorizontal: 6, paddingVertical: 2, borderRadius: radius.sm, fontWeight: '500' },
  taskDay: { fontSize: 10, color: colors.textMuted },
  taskOverdueText: { fontSize: 10, color: colors.danger, fontWeight: '700' },
  emptyState: { alignItems: 'center', backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xxxl, ...shadow.sm },
  emptyIcon: { width: 80, height: 80, borderRadius: 40, backgroundColor: colors.borderLight, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.lg },
  emptyTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text },
  emptySub: { fontSize: fontSize.sm, color: colors.textMuted, textAlign: 'center', marginTop: spacing.xs, lineHeight: 20 },
  emptyBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, backgroundColor: colors.primary, paddingHorizontal: spacing.xxl, paddingVertical: spacing.md, borderRadius: radius.lg, marginTop: spacing.xl },
  emptyBtnText: { fontSize: fontSize.md, fontWeight: '700', color: colors.white },
  cropCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, marginBottom: spacing.sm, ...shadow.sm },
  cropTop: { flexDirection: 'row', alignItems: 'center', gap: spacing.md },
  cropIconBox: { width: 44, height: 44, borderRadius: radius.md, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  cropName: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  cropMeta: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  cropProgress: { marginTop: spacing.md, paddingTop: spacing.md, borderTopWidth: 1, borderTopColor: colors.borderLight },
  cropTags: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginBottom: spacing.sm },
  stageTag: { backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 3, borderRadius: radius.full },
  stageTagText: { fontSize: 10, fontWeight: '600', color: '#15803d' },
  daysText: { fontSize: fontSize.xs, color: colors.textSecondary, fontWeight: '600' },
  progressWrap: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  progressBar: { flex: 1, height: 6, borderRadius: 3, backgroundColor: colors.borderLight, overflow: 'hidden' },
  progressFill: { height: 6, borderRadius: 3, backgroundColor: colors.primary },
  progressText: { fontSize: 10, fontWeight: '700', color: colors.primary },
  cropBottom: { flexDirection: 'row', gap: spacing.lg, marginTop: spacing.sm },
  cropInfoItem: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  cropInfoText: { fontSize: fontSize.xs, color: colors.textMuted },
});
