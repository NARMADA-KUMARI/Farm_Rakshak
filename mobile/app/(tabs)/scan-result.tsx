import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { cropApi } from '../../src/services/api';

export default function ScanResultScreen() {
  const router = useRouter();
  const { uploadId } = useLocalSearchParams<{ uploadId: string }>();
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!uploadId) return;
    (async () => {
      try {
        const res = await cropApi.getAnalysis(uploadId);
        setResult(res.data?.data || res.data);
      } catch (e) { console.log('Analysis error:', e); }
      finally { setLoading(false); }
    })();
  }, [uploadId]);

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={colors.primary} />
        <Text style={s.loadingText}>Loading analysis result...</Text>
      </SafeAreaView>
    );
  }

  const isHealthy = result?.diseaseName === 'Healthy' || !result?.diseaseName;
  const confidence = result?.confidence ? Math.round(result.confidence * 100) : 0;

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}>
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Ionicons name="arrow-back" size={22} color={colors.text} />
          </TouchableOpacity>
          <Text style={s.headerTitle}>Scan Result</Text>
          <View style={{ width: 40 }} />
        </View>

        {result ? (
          <>
            {/* Result Hero */}
            <View style={[s.resultCard, isHealthy ? s.resultHealthy : s.resultDiseased]}>
              <View style={[s.resultIcon, { backgroundColor: isHealthy ? 'rgba(255,255,255,0.2)' : 'rgba(255,255,255,0.2)' }]}>
                <Ionicons name={isHealthy ? 'shield-checkmark' : 'bug'} size={40} color="#ffffff" />
              </View>
              <Text style={s.resultTitle}>{isHealthy ? 'Healthy Plant! ✅' : result.diseaseName}</Text>
              <Text style={s.resultSub}>{isHealthy ? 'Your plant appears to be healthy' : 'Disease detected in your crop'}</Text>
              <View style={s.confidenceBar}>
                <Text style={s.confidenceLabel}>Confidence</Text>
                <View style={s.confidenceBg}>
                  <View style={[s.confidenceFill, { width: `${confidence}%` }]} />
                </View>
                <Text style={s.confidenceValue}>{confidence}%</Text>
              </View>
            </View>

            {/* Crop Info */}
            {result.cropName && (
              <View style={s.infoCard}>
                <Ionicons name="leaf" size={18} color={colors.primary} />
                <Text style={s.infoLabel}>Crop:</Text>
                <Text style={s.infoValue}>{result.cropName}</Text>
              </View>
            )}

            {/* Treatment / Recommendations */}
            {result.treatment && (
              <View style={s.section}>
                <View style={s.sectionHeader}>
                  <Ionicons name="medkit-outline" size={18} color="#dc2626" />
                  <Text style={s.sectionTitle}>Treatment</Text>
                </View>
                <Text style={s.sectionBody}>{result.treatment}</Text>
              </View>
            )}

            {result.prevention && (
              <View style={s.section}>
                <View style={s.sectionHeader}>
                  <Ionicons name="shield-outline" size={18} color={colors.primary} />
                  <Text style={s.sectionTitle}>Prevention</Text>
                </View>
                <Text style={s.sectionBody}>{result.prevention}</Text>
              </View>
            )}

            {result.recommendations && (
              <View style={s.section}>
                <View style={s.sectionHeader}>
                  <Ionicons name="bulb-outline" size={18} color={colors.warning} />
                  <Text style={s.sectionTitle}>Recommendations</Text>
                </View>
                <Text style={s.sectionBody}>{result.recommendations}</Text>
              </View>
            )}

            {/* Actions */}
            <View style={s.actions}>
              <TouchableOpacity style={s.actionBtn} onPress={() => router.push('/(tabs)/scan')}>
                <Ionicons name="camera-outline" size={20} color={colors.white} />
                <Text style={s.actionText}>Scan Another</Text>
              </TouchableOpacity>
              <TouchableOpacity style={s.actionBtnOutline} onPress={() => router.push('/advisory')}>
                <Ionicons name="bulb-outline" size={20} color={colors.primary} />
                <Text style={s.actionTextOutline}>Get Advisory</Text>
              </TouchableOpacity>
            </View>
          </>
        ) : (
          <View style={s.emptyState}>
            <Ionicons name="alert-circle-outline" size={56} color={colors.border} />
            <Text style={s.emptyText}>Analysis result not found</Text>
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
  loadingText: { color: colors.textMuted, marginTop: 12, fontSize: fontSize.sm },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: spacing.md, marginBottom: spacing.xl },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  headerTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text },
  resultCard: { borderRadius: radius.xl, padding: spacing.xxl, alignItems: 'center', marginBottom: spacing.xl, ...shadow.lg },
  resultHealthy: { backgroundColor: '#059669' },
  resultDiseased: { backgroundColor: '#dc2626' },
  resultIcon: { width: 80, height: 80, borderRadius: 40, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.lg },
  resultTitle: { fontSize: fontSize.xxl, fontWeight: '800', color: '#ffffff' },
  resultSub: { fontSize: fontSize.sm, color: 'rgba(255,255,255,0.8)', marginTop: spacing.xs },
  confidenceBar: { width: '100%', marginTop: spacing.xl },
  confidenceLabel: { fontSize: fontSize.xs, color: 'rgba(255,255,255,0.6)', marginBottom: spacing.xs },
  confidenceBg: { height: 8, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.2)', overflow: 'hidden' },
  confidenceFill: { height: 8, borderRadius: 4, backgroundColor: '#ffffff' },
  confidenceValue: { fontSize: fontSize.sm, fontWeight: '700', color: '#ffffff', marginTop: spacing.xs, textAlign: 'right' },
  infoCard: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, marginBottom: spacing.lg, ...shadow.sm },
  infoLabel: { fontSize: fontSize.sm, color: colors.textMuted },
  infoValue: { fontSize: fontSize.md, fontWeight: '700', color: colors.text },
  section: { backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.xl, marginBottom: spacing.md, ...shadow.sm },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.md },
  sectionTitle: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  sectionBody: { fontSize: fontSize.sm, color: colors.textSecondary, lineHeight: 22 },
  actions: { gap: spacing.md, marginTop: spacing.md },
  actionBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.primary, borderRadius: radius.lg, paddingVertical: spacing.lg, ...shadow.sm },
  actionText: { fontSize: fontSize.base, fontWeight: '700', color: colors.white },
  actionBtnOutline: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.white, borderRadius: radius.lg, paddingVertical: spacing.lg, borderWidth: 1, borderColor: colors.primary },
  actionTextOutline: { fontSize: fontSize.base, fontWeight: '700', color: colors.primary },
  emptyState: { alignItems: 'center', paddingVertical: 80 },
  emptyText: { fontSize: fontSize.md, color: colors.textMuted, marginTop: spacing.md },
});
