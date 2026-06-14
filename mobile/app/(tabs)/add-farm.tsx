import { View, Text, ScrollView, StyleSheet, TouchableOpacity, TextInput, ActivityIndicator, KeyboardAvoidingView, Platform } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { farmApi } from '../../src/services/api';

const soilTypes = ['Alluvial', 'Black (Regur)', 'Red', 'Laterite', 'Desert (Arid)', 'Mountain', 'Clay', 'Sandy', 'Loamy'];

export default function AddFarmScreen() {
  const router = useRouter();
  const [form, setForm] = useState({ farmName: '', village: '', district: '', state: '', totalArea: '', areaUnit: 'acres', soilType: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const update = (key: string, val: string) => setForm(prev => ({ ...prev, [key]: val }));

  const handleCreate = async () => {
    if (!form.farmName.trim()) { setError('Farm name is required'); return; }
    if (!form.totalArea.trim()) { setError('Total area is required'); return; }
    setError(''); setLoading(true);
    try {
      await farmApi.create({ ...form, totalArea: parseFloat(form.totalArea) });
      router.back();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to create farm');
    } finally { setLoading(false); }
  };

  return (
    <SafeAreaView style={s.safe}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView style={s.container} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
          {/* Header */}
          <View style={s.header}>
            <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
              <Ionicons name="close" size={22} color={colors.text} />
            </TouchableOpacity>
            <Text style={s.headerTitle}>Create Farm</Text>
            <View style={{ width: 40 }} />
          </View>

          {/* Hero */}
          <View style={s.hero}>
            <View style={s.heroIcon}>
              <Ionicons name="business" size={32} color={colors.white} />
            </View>
            <Text style={s.heroTitle}>Add Your Farm</Text>
            <Text style={s.heroSub}>Enter your farm details to start managing crops and get AI advisory</Text>
          </View>

          {error ? (
            <View style={s.errorBox}>
              <Ionicons name="alert-circle" size={16} color={colors.danger} />
              <Text style={s.errorText}>{error}</Text>
            </View>
          ) : null}

          {/* Form */}
          <View style={s.formCard}>
            <Text style={s.label}>Farm Name *</Text>
            <TextInput style={s.input} placeholder="e.g. Green Valley Farm" placeholderTextColor={colors.textMuted}
              value={form.farmName} onChangeText={(v) => update('farmName', v)} />

            <Text style={s.label}>Village</Text>
            <TextInput style={s.input} placeholder="e.g. Kondhanpur" placeholderTextColor={colors.textMuted}
              value={form.village} onChangeText={(v) => update('village', v)} />

            <Text style={s.label}>District</Text>
            <TextInput style={s.input} placeholder="e.g. Pune" placeholderTextColor={colors.textMuted}
              value={form.district} onChangeText={(v) => update('district', v)} />

            <Text style={s.label}>State</Text>
            <TextInput style={s.input} placeholder="e.g. Maharashtra" placeholderTextColor={colors.textMuted}
              value={form.state} onChangeText={(v) => update('state', v)} />

            <View style={s.row}>
              <View style={{ flex: 2 }}>
                <Text style={s.label}>Total Area *</Text>
                <TextInput style={s.input} placeholder="e.g. 5" placeholderTextColor={colors.textMuted}
                  value={form.totalArea} onChangeText={(v) => update('totalArea', v)} keyboardType="decimal-pad" />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={s.label}>Unit</Text>
                <View style={s.unitRow}>
                  {['acres', 'hectares'].map(u => (
                    <TouchableOpacity key={u} style={[s.unitBtn, form.areaUnit === u && s.unitBtnActive]} onPress={() => update('areaUnit', u)}>
                      <Text style={[s.unitText, form.areaUnit === u && s.unitTextActive]}>{u}</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>
            </View>

            <Text style={s.label}>Soil Type</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.soilScroll}>
              {soilTypes.map(soil => (
                <TouchableOpacity key={soil} style={[s.soilChip, form.soilType === soil && s.soilChipActive]} onPress={() => update('soilType', soil)}>
                  <Text style={[s.soilText, form.soilType === soil && s.soilTextActive]}>{soil}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>

          {/* Submit */}
          <TouchableOpacity style={[s.submitBtn, loading && s.submitDisabled]} onPress={handleCreate} disabled={loading} activeOpacity={0.8}>
            {loading ? <ActivityIndicator color={colors.white} /> : (
              <>
                <Ionicons name="checkmark-circle" size={20} color={colors.white} />
                <Text style={s.submitText}>Create Farm</Text>
              </>
            )}
          </TouchableOpacity>

          <View style={{ height: 40 }} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: spacing.md, marginBottom: spacing.xl },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  headerTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text },
  hero: { alignItems: 'center', marginBottom: spacing.xxl },
  heroIcon: { width: 72, height: 72, borderRadius: radius.xl, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', ...shadow.lg },
  heroTitle: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text, marginTop: spacing.lg },
  heroSub: { fontSize: fontSize.sm, color: colors.textMuted, textAlign: 'center', marginTop: spacing.xs },
  errorBox: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, padding: spacing.md, backgroundColor: colors.dangerLight, borderRadius: radius.md, marginBottom: spacing.lg },
  errorText: { fontSize: fontSize.sm, color: colors.danger, flex: 1 },
  formCard: { backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xl, ...shadow.sm, marginBottom: spacing.xl },
  label: { fontSize: fontSize.sm, fontWeight: '600', color: colors.text, marginTop: spacing.lg, marginBottom: spacing.sm },
  input: { backgroundColor: colors.borderLight, borderRadius: radius.md, paddingHorizontal: spacing.lg, paddingVertical: spacing.md + 2, fontSize: fontSize.base, color: colors.text, borderWidth: 1, borderColor: colors.border },
  row: { flexDirection: 'row', gap: spacing.md },
  unitRow: { flexDirection: 'row', gap: spacing.sm },
  unitBtn: { flex: 1, paddingVertical: spacing.md, borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, alignItems: 'center' },
  unitBtnActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  unitText: { fontSize: fontSize.xs, fontWeight: '600', color: colors.textSecondary },
  unitTextActive: { color: colors.white },
  soilScroll: { marginTop: spacing.xs },
  soilChip: { paddingHorizontal: spacing.lg, paddingVertical: spacing.sm + 2, borderRadius: radius.full, borderWidth: 1, borderColor: colors.border, marginRight: spacing.sm, backgroundColor: colors.borderLight },
  soilChipActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  soilText: { fontSize: fontSize.sm, color: colors.textSecondary },
  soilTextActive: { color: colors.white, fontWeight: '600' },
  submitBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.primary, borderRadius: radius.lg, paddingVertical: spacing.lg + 2, ...shadow.md },
  submitDisabled: { opacity: 0.6 },
  submitText: { fontSize: fontSize.base, fontWeight: '700', color: colors.white },
});
