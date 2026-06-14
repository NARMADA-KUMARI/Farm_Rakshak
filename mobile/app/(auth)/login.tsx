import { View, Text, TextInput, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, ActivityIndicator, ScrollView } from 'react-native';
import { useState } from 'react';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { authApi } from '../../src/services/api';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function LoginScreen() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async () => {
    if (!email || !password) { setError('Please fill all fields'); return; }
    setError(''); setLoading(true);
    try {
      const res = await authApi.login(email, password);
      const d = res.data?.data;
      const token = d?.accessToken || d?.token;
      if (token) {
        await AsyncStorage.setItem('token', token);
        await AsyncStorage.setItem('user', JSON.stringify({
          userId: d.userId, role: d.role, email,
        }));
        router.replace('/(tabs)/home');
      } else {
        setError('Invalid response from server');
      }
    } catch (e: any) {
      const msg = e?.response?.data?.error?.message || e?.response?.data?.message || 'Login failed. Check credentials.';
      setError(msg);
    } finally { setLoading(false); }
  };

  return (
    <KeyboardAvoidingView style={s.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView contentContainerStyle={s.scroll} keyboardShouldPersistTaps="handled">
        <View style={s.topDecor}><View style={s.circle1} /><View style={s.circle2} /></View>

        <View style={s.logoWrap}>
          <View style={s.logoBox}><Ionicons name="leaf" size={32} color={colors.white} /></View>
          <Text style={s.logoText}>Farm<Text style={s.logoAccent}>Rakshak</Text></Text>
          <Text style={s.logoSub}>AI-Powered Farm Management</Text>
        </View>

        <View style={s.card}>
          <Text style={s.cardTitle}>Welcome Back</Text>
          <Text style={s.cardSub}>Sign in to manage your farms</Text>

          {error ? (
            <View style={s.errorBox}>
              <Ionicons name="alert-circle" size={16} color={colors.danger} />
              <Text style={s.errorText}>{error}</Text>
            </View>
          ) : null}

          <View style={s.inputWrap}>
            <Ionicons name="mail-outline" size={20} color={colors.textMuted} style={s.inputIcon} />
            <TextInput style={s.input} placeholder="Email address" placeholderTextColor={colors.textMuted}
              value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" />
          </View>

          <View style={s.inputWrap}>
            <Ionicons name="lock-closed-outline" size={20} color={colors.textMuted} style={s.inputIcon} />
            <TextInput style={s.input} placeholder="Password" placeholderTextColor={colors.textMuted}
              value={password} onChangeText={setPassword} secureTextEntry={!showPassword} />
            <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={s.eyeBtn}>
              <Ionicons name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={20} color={colors.textMuted} />
            </TouchableOpacity>
          </View>

          <TouchableOpacity style={[s.btn, loading && s.btnDisabled]} onPress={handleLogin} disabled={loading} activeOpacity={0.8}>
            {loading ? <ActivityIndicator color={colors.white} /> : (
              <><Text style={s.btnText}>Sign In</Text><Ionicons name="arrow-forward" size={18} color={colors.white} /></>
            )}
          </TouchableOpacity>

          <View style={s.divider}><View style={s.dividerLine} /><Text style={s.dividerText}>OR</Text><View style={s.dividerLine} /></View>

          <TouchableOpacity style={s.googleBtn} activeOpacity={0.8}>
            <Ionicons name="logo-google" size={20} color="#4285F4" />
            <Text style={s.googleText}>Continue with Google</Text>
          </TouchableOpacity>

          <View style={s.registerWrap}>
            <Text style={s.registerText}>Don't have an account? </Text>
            <TouchableOpacity onPress={() => router.push('/(auth)/register')}>
              <Text style={s.registerLink}>Sign Up</Text>
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
  topDecor: { height: 200, position: 'relative', overflow: 'hidden' },
  circle1: { position: 'absolute', top: -80, right: -40, width: 200, height: 200, borderRadius: 100, backgroundColor: colors.primaryLight, opacity: 0.6 },
  circle2: { position: 'absolute', top: -30, left: -60, width: 160, height: 160, borderRadius: 80, backgroundColor: '#a7f3d0', opacity: 0.4 },
  logoWrap: { alignItems: 'center', marginTop: -60 },
  logoBox: { width: 72, height: 72, borderRadius: 22, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', ...shadow.lg },
  logoText: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text, marginTop: spacing.md },
  logoAccent: { color: colors.primary },
  logoSub: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.xs },
  card: { marginHorizontal: spacing.xl, marginTop: spacing.xxl, backgroundColor: colors.card, borderRadius: radius.xl, padding: spacing.xxl, ...shadow.md },
  cardTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text },
  cardSub: { fontSize: fontSize.md, color: colors.textSecondary, marginTop: spacing.xs, marginBottom: spacing.xl },
  errorBox: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, padding: spacing.md, backgroundColor: colors.dangerLight, borderRadius: radius.md, marginBottom: spacing.lg },
  errorText: { fontSize: fontSize.sm, color: colors.danger, flex: 1 },
  inputWrap: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.borderLight, borderRadius: radius.md, marginBottom: spacing.md, borderWidth: 1, borderColor: colors.border },
  inputIcon: { paddingLeft: spacing.lg },
  input: { flex: 1, paddingVertical: spacing.lg, paddingHorizontal: spacing.md, fontSize: fontSize.base, color: colors.text },
  eyeBtn: { padding: spacing.lg },
  btn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.primary, borderRadius: radius.md, paddingVertical: spacing.lg, marginTop: spacing.sm, ...shadow.sm },
  btnDisabled: { opacity: 0.6 },
  btnText: { fontSize: fontSize.base, fontWeight: '700', color: colors.white },
  divider: { flexDirection: 'row', alignItems: 'center', marginVertical: spacing.xl },
  dividerLine: { flex: 1, height: 1, backgroundColor: colors.border },
  dividerText: { paddingHorizontal: spacing.md, fontSize: fontSize.xs, color: colors.textMuted, fontWeight: '600' },
  googleBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.md, backgroundColor: colors.white, borderRadius: radius.md, paddingVertical: spacing.lg, borderWidth: 1, borderColor: colors.border },
  googleText: { fontSize: fontSize.md, fontWeight: '600', color: colors.text },
  registerWrap: { flexDirection: 'row', justifyContent: 'center', marginTop: spacing.xl },
  registerText: { fontSize: fontSize.md, color: colors.textSecondary },
  registerLink: { fontSize: fontSize.md, fontWeight: '700', color: colors.primary },
});
