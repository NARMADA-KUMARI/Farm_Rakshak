import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { cropApi } from '../../src/services/api';

export default function CropDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [lifecycle, setLifecycle] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    (async () => {
      try {
        const res = await cropApi.getLifecycle(id);
        setLifecycle(res.data?.data || res.data);
      } catch (e) { console.log('Lifecycle error:', e); }
      finally { setLoading(false); }
    })();
  }, [id]);

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={colors.primary} />
        <Text style={s.loadingText}>Loading crop lifecycle...</Text>
      </SafeAreaView>
    );
  }

  const stages = lifecycle?.stages || [];
  const tasks = lifecycle?.todayTasks || lifecycle?.upcomingTasks || [];

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}>
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Ionicons name="arrow-back" size={22} color={colors.text} />
          </TouchableOpacity>
          <Text style={s.headerTitle}>{lifecycle?.cropName || 'Crop Details'}</Text>
          <View style={{ width: 40 }} />
        </View>

        {lifecycle && (
          <>
            {/* Crop Info */}
            <View style={s.infoCard}>
              <View style={s.cropIconBox}>
                <Ionicons name="leaf" size={28} color={colors.white} />
              </View>
              <Text style={s.cropName}>{lifecycle.cropName}</Text>
              {lifecycle.variety && <Text style={s.variety}>{lifecycle.variety}</Text>}
              <View style={s.infoRow}>
                {lifecycle.currentStage && (
                  <View style={s.stageChip}>
                    <Text style={s.stageText}>📌 {lifecycle.currentStage}</Text>
                  </View>
                )}
                {lifecycle.daysSinceSowing > 0 && (
                  <Text style={s.daysText}>Day {lifecycle.daysSinceSowing}</Text>
                )}
              </View>
            </View>

            {/* Growth Stages Timeline */}
            {stages.length > 0 && (
              <View style={s.section}>
                <Text style={s.sectionTitle}>Growth Stages</Text>
                {stages.map((stage: any, i: number) => {
                  const isActive = stage.isCurrent;
                  const isDone = stage.isCompleted;
                  return (
                    <View key={i} style={s.stageRow}>
                      <View style={s.timelineCol}>
                        <View style={[s.dot, isActive && s.dotActive, isDone && s.dotDone]} />
                        {i < stages.length - 1 && <View style={[s.line, isDone && s.lineDone]} />}
                      </View>
                      <View style={[s.stageCard, isActive && s.stageCardActive]}>
                        <Text style={[s.stageName, isActive && s.stageNameActive]}>{stage.stageName}</Text>
                        <Text style={s.stageDays}>Days {stage.startDay}–{stage.endDay}</Text>
                        {stage.description && <Text style={s.stageDesc}>{stage.description}</Text>}
                      </View>
                    </View>
                  );
                })}
              </View>
            )}

            {/* Today's Tasks */}
            {tasks.length > 0 && (
              <View style={s.section}>
                <Text style={s.sectionTitle}>Tasks</Text>
                {tasks.map((task: any, i: number) => (
                  <TouchableOpacity key={i} style={[s.taskCard, task.isOverdue && s.taskOverdue]} activeOpacity={0.7}
                    onPress={() => { if (!task.isCompleted) cropApi.completeTask(task.id).catch(() => {}); }}>
                    <View style={s.taskCheck}>
                      <Ionicons name={task.isCompleted ? 'checkmark-circle' : 'ellipse-outline'} size={22}
                        color={task.isCompleted ? colors.success : task.isOverdue ? colors.danger : colors.textMuted} />
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text style={[s.taskTitle, task.isCompleted && s.taskDone]}>{task.title}</Text>
                      <View style={s.taskMeta}>
                        {task.stage && <Text style={s.taskStage}>{task.stage}</Text>}
                        <Text style={s.taskDay}>Day {task.dayNumber}</Text>
                        {task.isOverdue && <Text style={s.taskOverdueText}>⚠️ Overdue</Text>}
                        {task.priority === 'HIGH' && <Text style={s.taskHigh}>HIGH</Text>}
                      </View>
                    </View>
                  </TouchableOpacity>
                ))}
              </View>
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
  loadingText: { color: colors.textMuted, marginTop: 12, fontSize: fontSize.sm },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: spacing.md, marginBottom: spacing.xl },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  headerTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text, flex: 1, textAlign: 'center' },
  infoCard: { backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xxl, alignItems: 'center', marginBottom: spacing.xl, ...shadow.md },
  cropIconBox: { width: 64, height: 64, borderRadius: radius.lg, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.md },
  cropName: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text },
  variety: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: 2 },
  infoRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginTop: spacing.md },
  stageChip: { backgroundColor: '#dcfce7', paddingHorizontal: 12, paddingVertical: 4, borderRadius: radius.full },
  stageText: { fontSize: fontSize.sm, fontWeight: '600', color: '#15803d' },
  daysText: { fontSize: fontSize.sm, color: colors.textSecondary, fontWeight: '600' },
  section: { marginBottom: spacing.xl },
  sectionTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text, marginBottom: spacing.lg },
  stageRow: { flexDirection: 'row', marginBottom: 0 },
  timelineCol: { width: 28, alignItems: 'center' },
  dot: { width: 14, height: 14, borderRadius: 7, backgroundColor: colors.border, borderWidth: 2, borderColor: colors.borderLight },
  dotActive: { backgroundColor: colors.primary, borderColor: colors.primaryLight, width: 18, height: 18, borderRadius: 9 },
  dotDone: { backgroundColor: colors.success, borderColor: colors.successLight },
  line: { width: 2, flex: 1, backgroundColor: colors.borderLight, minHeight: 20 },
  lineDone: { backgroundColor: colors.successLight },
  stageCard: { flex: 1, backgroundColor: colors.white, borderRadius: radius.md, padding: spacing.md, marginLeft: spacing.sm, marginBottom: spacing.sm, ...shadow.sm },
  stageCardActive: { borderWidth: 1, borderColor: colors.primary, backgroundColor: colors.primaryLight },
  stageName: { fontSize: fontSize.md, fontWeight: '600', color: colors.text },
  stageNameActive: { color: colors.primary, fontWeight: '700' },
  stageDays: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  stageDesc: { fontSize: fontSize.xs, color: colors.textSecondary, marginTop: 4 },
  taskCard: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md, backgroundColor: colors.white, borderRadius: radius.md, padding: spacing.md, marginBottom: spacing.sm, ...shadow.sm },
  taskOverdue: { backgroundColor: '#fef2f2', borderWidth: 1, borderColor: '#fecaca' },
  taskCheck: { marginTop: 2 },
  taskTitle: { fontSize: fontSize.md, fontWeight: '600', color: colors.text },
  taskDone: { textDecorationLine: 'line-through', color: colors.textMuted },
  taskMeta: { flexDirection: 'row', gap: spacing.sm, marginTop: 4, flexWrap: 'wrap' },
  taskStage: { fontSize: 10, backgroundColor: colors.borderLight, paddingHorizontal: 6, paddingVertical: 2, borderRadius: radius.sm, fontWeight: '500', color: colors.textSecondary },
  taskDay: { fontSize: 10, color: colors.textMuted },
  taskOverdueText: { fontSize: 10, color: colors.danger, fontWeight: '700' },
  taskHigh: { fontSize: 10, color: colors.danger, fontWeight: '700' },
});
