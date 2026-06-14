import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { userApi, farmApi } from '../../src/services/api';

const menuItems = [
  { icon: 'notifications-outline' as const, label: 'Notifications', color: colors.warning, route: '/notifications' },
  { icon: 'cloudy-outline' as const, label: 'Weather', color: colors.info, route: '/weather' },
  { icon: 'bulb-outline' as const, label: 'AI Advisory', color: colors.purple, route: '/advisory' },
  { icon: 'chatbubble-ellipses-outline' as const, label: 'AI Assistant', color: '#0891b2', route: '/ai-chat' },
  { icon: 'language-outline' as const, label: 'Language', color: colors.info, route: null },
  { icon: 'shield-checkmark-outline' as const, label: 'Privacy & Security', color: colors.purple, route: null },
  { icon: 'help-circle-outline' as const, label: 'Help & Support', color: colors.orange, route: null },
  { icon: 'information-circle-outline' as const, label: 'About FarmRakshak', color: colors.textSecondary, route: null },
];

export default function ProfileScreen() {
  const router = useRouter();
  const [user, setUser] = useState<any>(null);
  const [farmCount, setFarmCount] = useState(0);
  const [cropCount, setCropCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const userStr = await AsyncStorage.getItem('user');
        if (userStr) setUser(JSON.parse(userStr));

        const [profileRes, farmsRes] = await Promise.allSettled([
          userApi.getProfile(),
          farmApi.list(),
        ]);

        if (profileRes.status === 'fulfilled') {
          const p = profileRes.value.data?.data;
          if (p) setUser(p);
        }
        if (farmsRes.status === 'fulfilled') {
          const farms = farmsRes.value.data?.data || [];
          setFarmCount(farms.length);
          setCropCount(farms.reduce((s: number, f: any) => s + (f.cropCount || 0), 0));
        }
      } catch (e) { console.log('Profile error:', e); }
      finally { setLoading(false); }
    })();
  }, []);

  const handleLogout = async () => {
    await AsyncStorage.multiRemove(['token', 'user']);
    router.replace('/(auth)/login');
  };

  const displayName = user?.name || user?.displayName || user?.email?.split('@')[0] || 'Farmer';
  const initial = displayName.charAt(0).toUpperCase();

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]} edges={['top']}>
        <ActivityIndicator size="large" color={colors.primary} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}>
        {/* Profile Header */}
        <View style={s.profileCard}>
          <View style={s.avatar}><Text style={s.avatarText}>{initial}</Text></View>
          <Text style={s.profileName}>{displayName}</Text>
          <Text style={s.profileEmail}>{user?.email || user?.mobile || ''}</Text>
          {user?.village && (
            <View style={s.locationRow}>
              <Ionicons name="location-outline" size={13} color={colors.textMuted} />
              <Text style={s.locationText}>{user.village}, {user.district || user.state || ''}</Text>
            </View>
          )}
          <View style={s.profileStats}>
            <View style={s.profileStat}>
              <Text style={s.profileStatValue}>{farmCount}</Text>
              <Text style={s.profileStatLabel}>Farms</Text>
            </View>
            <View style={s.statDivider} />
            <View style={s.profileStat}>
              <Text style={s.profileStatValue}>{cropCount}</Text>
              <Text style={s.profileStatLabel}>Crops</Text>
            </View>
            <View style={s.statDivider} />
            <View style={s.profileStat}>
              <Text style={s.profileStatValue}>{user?.languagePreference || 'EN'}</Text>
              <Text style={s.profileStatLabel}>Language</Text>
            </View>
          </View>
        </View>

        {/* Menu Items */}
        <View style={s.menuCard}>
          {menuItems.map((item, i) => (
            <TouchableOpacity key={item.label}
              style={[s.menuItem, i < menuItems.length - 1 && s.menuBorder]}
              activeOpacity={0.6}
              onPress={() => item.route ? router.push(item.route as any) : null}
            >
              <View style={[s.menuIcon, { backgroundColor: `${item.color}15` }]}>
                <Ionicons name={item.icon} size={20} color={item.color} />
              </View>
              <Text style={s.menuLabel}>{item.label}</Text>
              <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
            </TouchableOpacity>
          ))}
        </View>

        {/* Logout */}
        <TouchableOpacity style={s.logoutBtn} activeOpacity={0.7} onPress={handleLogout}>
          <Ionicons name="log-out-outline" size={20} color={colors.danger} />
          <Text style={s.logoutText}>Sign Out</Text>
        </TouchableOpacity>

        <Text style={s.version}>FarmRakshak v1.0.0</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl, paddingTop: spacing.xl },
  profileCard: { backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xxl, alignItems: 'center', marginBottom: spacing.xl, ...shadow.md },
  avatar: { width: 80, height: 80, borderRadius: 40, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.md },
  avatarText: { fontSize: fontSize.xxxl, fontWeight: '800', color: colors.white },
  profileName: { fontSize: fontSize.xl, fontWeight: '800', color: colors.text },
  profileEmail: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: 4 },
  locationRow: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: spacing.sm },
  locationText: { fontSize: fontSize.xs, color: colors.textMuted },
  profileStats: { flexDirection: 'row', marginTop: spacing.xl, gap: spacing.xxl },
  profileStat: { alignItems: 'center' },
  profileStatValue: { fontSize: fontSize.xl, fontWeight: '800', color: colors.text },
  profileStatLabel: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  statDivider: { width: 1, height: 32, backgroundColor: colors.border },
  menuCard: { backgroundColor: colors.white, borderRadius: radius.xl, ...shadow.sm, marginBottom: spacing.xl },
  menuItem: { flexDirection: 'row', alignItems: 'center', padding: spacing.lg },
  menuBorder: { borderBottomWidth: 1, borderBottomColor: colors.borderLight },
  menuIcon: { width: 36, height: 36, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center', marginRight: spacing.md },
  menuLabel: { flex: 1, fontSize: fontSize.md, fontWeight: '600', color: colors.text },
  logoutBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.dangerLight, borderRadius: radius.lg, paddingVertical: spacing.lg, marginBottom: spacing.lg },
  logoutText: { fontSize: fontSize.md, fontWeight: '700', color: colors.danger },
  version: { textAlign: 'center', fontSize: fontSize.xs, color: colors.textMuted, marginBottom: spacing.xxxl },
});
