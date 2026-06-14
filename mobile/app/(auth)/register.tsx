import { View, Text, TextInput, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, ActivityIndicator, ScrollView } from 'react-native';
import { useState } from 'react';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';

export default function RegisterScreen() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mobile, setMobile] = useState('');
  const [loading, setLoading] = useState(false);

  return (
    <KeyboardAvoidingView style={s.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView contentContainerStyle={s.scroll} keyboardShouldPersistTaps="handled">
        <View style={s.topDecor}><View style={s.circle1} /><View style={s.circle2} /></View>

        <View style={s.logoWrap}>
          <View style={s.logoBox}><Ionicons name="leaf" size={28} color={colors.white} /></View>
          <Text style={s.logoText}>Create Account</Text>
          <Text style={s.logoSub}>Join FarmRakshak today</Text>
        </View>

        <View style={s.card}>
          {[
            { icon: 'person-outline' as const, placeholder: 'Full Name', value: name, setter: setName },
            { icon: 'mail-outline' as const, placeholder: 'Email', value: email, setter: setEmail, kb: 'email-address' as const },
            { icon: 'call-outline' as const, placeholder: 'Mobile Number', value: mobile, setter: setMobile, kb: 'phone-pad' as const },
          ].map(f => (
            <View key={f.placeholder} style={s.inputWrap}>
              <Ionicons name={f.icon} size={20} color={colors.textMuted} style={s.inputIcon} />
              <TextInput style={s.input} placeholder={f.placeholder} placeholderTextColor={colors.textMuted}
                value={f.value} onChangeText={f.setter} keyboardType={f.kb || 'default'} autoCapitalize="none" />
            </View>
          ))}

          <View style={s.inputWrap}>
            <Ionicons name="lock-closed-outline" size={20} color={colors.textMuted} style={s.inputIcon} />
            <TextInput style={s.input} placeholder="Password" placeholderTextColor={colors.textMuted}
              value={password} onChangeText={setPassword} secureTextEntry />
          </View>

          <TouchableOpacity style={s.btn} activeOpacity={0.8} disabled={loading}>
            {loading ? <ActivityIndicator color={colors.white} /> : (
              <><Text style={s.btnText}>Create Account</Text><Ionicons name="arrow-forward" size={18} color={colors.white} /></>
            )}
          </TouchableOpacity>

          <View style={s.loginWrap}>
            <Text style={s.loginText}>Already have an account? </Text>
            <TouchableOpacity onPress={() => router.back()}>
              <Text style={s.loginLink}>Sign In</Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  scroll: { flexGrow: 1, paddingBottom: 40 },
  topDecor: { height: 160, position: 'relative', overflow: 'hidden' },
  circle1: { position: 'absolute', top: -60, right: -30, width: 160, height: 160, borderRadius: 80, backgroundColor: colors.primaryLight, opacity: 0.6 },
  circle2: { position: 'absolute', top: -20, left: -40, width: 120, height: 120, borderRadius: 60, backgroundColor: '#a7f3d0', opacity: 0.4 },
  logoWrap: { alignItems: 'center', marginTop: -40 },
  logoBox: { width: 64, height: 64, borderRadius: 20, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', ...shadow.lg },
  logoText: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text, marginTop: spacing.md },
  logoSub: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.xs },
  card: { marginHorizontal: spacing.xl, marginTop: spacing.xxl, backgroundColor: colors.card, borderRadius: radius.xl, padding: spacing.xxl, ...shadow.md },
  inputWrap: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.borderLight, borderRadius: radius.md, marginBottom: spacing.md, borderWidth: 1, borderColor: colors.border },
  inputIcon: { paddingLeft: spacing.lg },
  input: { flex: 1, paddingVertical: spacing.lg, paddingHorizontal: spacing.md, fontSize: fontSize.base, color: colors.text },
  btn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.primary, borderRadius: radius.md, paddingVertical: spacing.lg, marginTop: spacing.sm },
  btnText: { fontSize: fontSize.base, fontWeight: '700', color: colors.white },
  loginWrap: { flexDirection: 'row', justifyContent: 'center', marginTop: spacing.xl },
  loginText: { fontSize: fontSize.md, color: colors.textSecondary },
  loginLink: { fontSize: fontSize.md, fontWeight: '700', color: colors.primary },
});
