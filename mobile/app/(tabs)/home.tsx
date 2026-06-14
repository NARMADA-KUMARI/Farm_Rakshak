import { View, Text, ScrollView, StyleSheet, TouchableOpacity, RefreshControl, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { farmApi, weatherApi, notificationApi, userApi, cropApi } from '../../src/services/api';

const quickActions = [
  { icon: 'add-circle-outline' as const, label: 'Add Farm', color: colors.primary, bg: colors.primaryLight, route: '/add-farm' },
  { icon: 'camera-outline' as const, label: 'Scan Disease', color: colors.danger, bg: colors.dangerLight, route: '/(tabs)/scan' },
  { icon: 'trending-up-outline' as const, label: 'Market Prices', color: colors.orange, bg: colors.orangeLight, route: '/(tabs)/market' },
  { icon: 'chatbubble-ellipses-outline' as const, label: 'AI Assistant', color: colors.purple, bg: colors.purpleLight, route: '/ai-chat' },
];

const platformFeatures = [
  { icon: 'leaf-outline' as const, title: 'Crop Lifecycle', desc: 'Track growth & daily tasks', color: '#059669', bg: '#ecfdf5', route: '/(tabs)/farms' },
  { icon: 'cloud-outline' as const, title: 'Weather', desc: 'Real-time forecasts', color: '#3b82f6', bg: '#eff6ff', route: '/weather' },
  { icon: 'bulb-outline' as const, title: 'AI Advisory', desc: 'Smart farming tips', color: '#8b5cf6', bg: '#f5f3ff', route: '/advisory' },
  { icon: 'notifications-outline' as const, title: 'Alerts', desc: 'Proactive alerts', color: '#ec4899', bg: '#fdf2f8', route: '/notifications' },
];

export default function HomeScreen() {
  const router = useRouter();
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [userName, setUserName] = useState('Farmer');
  const [farms, setFarms] = useState<any[]>([]);
  const [weather, setWeather] = useState<any>(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [todayTasks, setTodayTasks] = useState<any[]>([]);

  const getGreeting = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good Morning 🌾';
    if (h < 17) return 'Good Afternoon ☀️';
    return 'Good Evening 🌙';
  };

  const today = new Date().toLocaleDateString('en-IN', { weekday: 'long', month: 'long', day: 'numeric' });

  const fetchData = async () => {
    try {
      const userStr = await AsyncStorage.getItem('user');
      if (userStr) {
        const u = JSON.parse(userStr);
        setUserName(u.name || u.displayName || u.email?.split('@')[0] || 'Farmer');
      }

      const [farmsRes, weatherRes, notifRes, profileRes, tasksRes] = await Promise.allSettled([
        farmApi.list(),
        weatherApi.getCurrent(),
        notificationApi.unreadCount(),
        userApi.getProfile(),
        cropApi.getTodaysTasks(),
      ]);

      if (farmsRes.status === 'fulfilled') setFarms(farmsRes.value.data?.data || []);
      if (weatherRes.status === 'fulfilled') setWeather(weatherRes.value.data?.data || weatherRes.value.data);
      if (notifRes.status === 'fulfilled') setUnreadCount(notifRes.value.data?.data || 0);
      if (profileRes.status === 'fulfilled') {
        const p = profileRes.value.data?.data;
        if (p?.name || p?.displayName) setUserName(p.name || p.displayName);
      }
      if (tasksRes.status === 'fulfilled') setTodayTasks(tasksRes.value.data?.data || []);
    } catch (e) { console.log('Dashboard fetch error:', e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);

  const onRefresh = () => { setRefreshing(true); fetchData().then(() => setRefreshing(false)); };

  const totalCrops = farms.reduce((sum: number, f: any) => sum + (f.cropCount || 0), 0);

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]} edges={['top']}>
        <ActivityIndicator size="large" color={colors.primary} />
        <Text style={{ color: colors.textMuted, marginTop: 12, fontSize: fontSize.sm }}>Loading dashboard...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>

        {/* Header */}
        <View style={s.header}>
          <View style={{ flex: 1 }}>
            <Text style={s.dateText}>{today}</Text>
            <Text style={s.greeting}>{getGreeting()}</Text>
            <Text style={s.name}>Welcome, {userName}!</Text>
          </View>
          <TouchableOpacity style={s.bellBtn} onPress={() => router.push('/notifications')}>
            <Ionicons name="notifications-outline" size={24} color={colors.text} />
            {unreadCount > 0 && <View style={s.badge}><Text style={s.badgeText}>{unreadCount > 9 ? '9+' : unreadCount}</Text></View>}
          </TouchableOpacity>
        </View>

        {/* Weather Banner */}
        {weather ? (
          <TouchableOpacity style={s.weatherBanner} activeOpacity={0.8} onPress={() => router.push('/weather')}>
            <View style={s.heroCircle} />
            <View style={s.weatherTop}>
              <View>
                <Text style={s.weatherTemp}>{Math.round(weather.temperature || 0)}°C</Text>
                <Text style={s.weatherDesc}>{weather.description || 'Clear'}</Text>
              </View>
              <View style={s.weatherRight}>
                <Ionicons name="sunny" size={36} color="rgba(255,255,255,0.4)" />
                <Text style={s.weatherTap}>Tap for details →</Text>
              </View>
            </View>
            <View style={s.weatherRow}>
              <View style={s.weatherItem}>
                <Ionicons name="water-outline" size={14} color="rgba(255,255,255,0.8)" />
                <Text style={s.weatherVal}>{weather.humidity || 0}% Humidity</Text>
              </View>
              <View style={s.weatherItem}>
                <Ionicons name="speedometer-outline" size={14} color="rgba(255,255,255,0.8)" />
                <Text style={s.weatherVal}>{weather.windSpeed || 0} km/h Wind</Text>
              </View>
            </View>
          </TouchableOpacity>
        ) : (
          <TouchableOpacity style={s.heroBanner} activeOpacity={0.8} onPress={() => router.push('/advisory')}>
            <View style={s.heroCircle} />
            <Text style={s.heroTitle}>AI Crop Recommendations</Text>
            <Text style={s.heroSub}>Get personalized crop suggestions based on your location, soil & season</Text>
            <View style={s.heroBtn}>
              <Text style={s.heroBtnText}>Explore</Text>
              <Ionicons name="arrow-forward" size={16} color={colors.white} />
            </View>
          </TouchableOpacity>
        )}

        {/* Quick Actions */}
        <Text style={s.sectionTitle}>Quick Actions</Text>
        <View style={s.actionsGrid}>
          {quickActions.map(a => (
            <TouchableOpacity key={a.label} style={s.actionCard} activeOpacity={0.7}
              onPress={() => router.push(a.route as any)}>
              <View style={[s.actionIcon, { backgroundColor: a.bg }]}>
                <Ionicons name={a.icon} size={24} color={a.color} />
              </View>
              <Text style={s.actionLabel}>{a.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Stats */}
        <Text style={s.sectionTitle}>Your Farm Stats</Text>
        <View style={s.statsRow}>
          <View style={s.statCard}>
            <Ionicons name="business" size={20} color={colors.primary} />
            <Text style={s.statValue}>{farms.length}</Text>
            <Text style={s.statLabel}>Farms</Text>
          </View>
          <View style={s.statCard}>
            <Ionicons name="leaf" size={20} color={colors.accent} />
            <Text style={s.statValue}>{totalCrops}</Text>
            <Text style={s.statLabel}>Crops</Text>
          </View>
          <View style={s.statCard}>
            <Ionicons name="shield-checkmark" size={20} color={colors.info} />
            <Text style={s.statValue}>98%</Text>
            <Text style={s.statLabel}>Health</Text>
          </View>
        </View>

        {/* Today's Tasks */}
        {todayTasks.length > 0 && (
          <>
            <View style={s.sectionHeader}>
              <View style={s.sectionLeft}>
                <View style={[s.sectionIcon, { backgroundColor: colors.warningLight }]}>
                  <Ionicons name="checkbox-outline" size={16} color={colors.warning} />
                </View>
                <Text style={s.sectionTitle2}>Today's Tasks</Text>
              </View>
              <Text style={s.taskCount}>{todayTasks.length} pending</Text>
            </View>
            {todayTasks.slice(0, 4).map((task: any) => (
              <TouchableOpacity key={task.id} style={[s.taskCard, task.isOverdue && s.taskOverdue]} activeOpacity={0.7}
                onPress={() => cropApi.completeTask(task.id).then(() => fetchData()).catch(() => {})}>
                <Ionicons name="ellipse-outline" size={20} color={task.isOverdue ? colors.danger : colors.textMuted} />
                <View style={{ flex: 1 }}>
                  <Text style={s.taskTitle}>{task.title}</Text>
                  <View style={s.taskMeta}>
                    {task.stage && <Text style={s.taskStage}>{task.stage}</Text>}
                    <Text style={s.taskDay}>Day {task.dayNumber}</Text>
                    {task.isOverdue && <Text style={s.taskOverdueText}>⚠️ Overdue</Text>}
                  </View>
                </View>
              </TouchableOpacity>
            ))}
          </>
        )}

        {/* My Farms preview */}
        {farms.length > 0 && (
          <>
            <View style={s.sectionHeader}>
              <Text style={s.sectionTitle}>My Farms</Text>
              <TouchableOpacity onPress={() => router.push('/(tabs)/farms')}>
                <Text style={s.viewAll}>View all →</Text>
              </TouchableOpacity>
            </View>
            {farms.slice(0, 3).map((farm: any) => (
              <TouchableOpacity key={farm.id} style={s.farmMiniCard} activeOpacity={0.7}
                onPress={() => router.push({ pathname: '/farm-detail', params: { id: farm.id } })}>
                <View style={s.farmMiniIcon}><Ionicons name="business" size={18} color={colors.primary} /></View>
                <View style={{ flex: 1 }}>
                  <Text style={s.farmMiniName}>{farm.farmName}</Text>
                  <Text style={s.farmMiniMeta}>{farm.village}{farm.district ? `, ${farm.district}` : ''} · {farm.totalArea} {farm.areaUnit || 'acres'}</Text>
                </View>
                <View style={s.farmMiniBadge}>
                  <Text style={s.farmMiniBadgeText}>{farm.cropCount || 0} crops</Text>
                </View>
                <Ionicons name="chevron-forward" size={16} color={colors.textMuted} style={{ marginLeft: 4 }} />
              </TouchableOpacity>
            ))}
          </>
        )}

        {farms.length === 0 && (
          <View style={s.emptyFarms}>
            <Ionicons name="business-outline" size={48} color={colors.border} />
            <Text style={s.emptyTitle}>No Farms Yet</Text>
            <Text style={s.emptySub}>Create your first farm to start managing crops</Text>
            <TouchableOpacity style={s.emptyBtn} onPress={() => router.push('/add-farm')} activeOpacity={0.8}>
              <Ionicons name="add" size={18} color={colors.white} />
              <Text style={s.emptyBtnText}>Create Farm</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Platform Features Grid */}
        <Text style={[s.sectionTitle, { marginTop: spacing.xl }]}>Platform Features</Text>
        <View style={s.featuresGrid}>
          {platformFeatures.map(f => (
            <TouchableOpacity key={f.title} style={s.featureCard} activeOpacity={0.7}
              onPress={() => router.push(f.route as any)}>
              <View style={[s.featureIcon, { backgroundColor: f.bg }]}>
                <Ionicons name={f.icon} size={22} color={f.color} />
              </View>
              <Text style={s.featureTitle}>{f.title}</Text>
              <Text style={s.featureDesc}>{f.desc}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* AI Banner */}
        <TouchableOpacity style={s.aiBanner} activeOpacity={0.8} onPress={() => router.push('/ai-chat')}>
          <View style={s.aiBannerIcon}>
            <Ionicons name="sparkles" size={24} color={colors.white} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={s.aiBannerTitle}>AI Farming Assistant</Text>
            <Text style={s.aiBannerSub}>Ask anything about crops, pests, soil, or weather</Text>
          </View>
          <Ionicons name="arrow-forward-circle" size={28} color={colors.purple} />
        </TouchableOpacity>

        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: spacing.lg, marginBottom: spacing.xl },
  dateText: { fontSize: fontSize.xs, color: colors.textMuted, fontWeight: '500' },
  greeting: { fontSize: fontSize.md, color: colors.textSecondary, marginTop: 2 },
  name: { fontSize: fontSize.xl, fontWeight: '800', color: colors.text, marginTop: 2 },
  bellBtn: { width: 48, height: 48, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  badge: { position: 'absolute', top: 6, right: 6, minWidth: 18, height: 18, borderRadius: 9, backgroundColor: colors.danger, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 4 },
  badgeText: { fontSize: 9, fontWeight: '800', color: colors.white },
  weatherBanner: { backgroundColor: colors.primary, borderRadius: radius.xl, padding: spacing.xxl, marginBottom: spacing.xxl, overflow: 'hidden', ...shadow.md },
  weatherTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: spacing.md },
  weatherTemp: { fontSize: 40, fontWeight: '800', color: colors.white },
  weatherDesc: { fontSize: fontSize.base, color: 'rgba(255,255,255,0.8)', fontWeight: '600' },
  weatherRight: { alignItems: 'flex-end' },
  weatherTap: { fontSize: fontSize.xs, color: 'rgba(255,255,255,0.5)', marginTop: 4 },
  weatherRow: { flexDirection: 'row', gap: spacing.xl },
  weatherItem: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  weatherVal: { fontSize: fontSize.xs, color: 'rgba(255,255,255,0.8)', fontWeight: '500' },
  heroBanner: { backgroundColor: colors.primary, borderRadius: radius.xl, padding: spacing.xxl, marginBottom: spacing.xxl, overflow: 'hidden', ...shadow.md },
  heroCircle: { position: 'absolute', top: -30, right: -30, width: 120, height: 120, borderRadius: 60, backgroundColor: 'rgba(255,255,255,0.1)' },
  heroTitle: { fontSize: fontSize.lg, fontWeight: '800', color: colors.white },
  heroSub: { fontSize: fontSize.sm, color: 'rgba(255,255,255,0.8)', marginTop: spacing.sm, lineHeight: 20 },
  heroBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, backgroundColor: 'rgba(255,255,255,0.2)', alignSelf: 'flex-start', paddingHorizontal: spacing.lg, paddingVertical: spacing.sm, borderRadius: radius.full, marginTop: spacing.lg },
  heroBtnText: { fontSize: fontSize.sm, fontWeight: '700', color: colors.white },
  sectionTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text, marginBottom: spacing.md },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.md, marginTop: spacing.md },
  sectionLeft: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  sectionIcon: { width: 28, height: 28, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  sectionTitle2: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  viewAll: { fontSize: fontSize.sm, color: colors.primary, fontWeight: '600' },
  actionsGrid: { flexDirection: 'row', gap: spacing.md, marginBottom: spacing.xxl },
  actionCard: { flex: 1, backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, alignItems: 'center', ...shadow.sm },
  actionIcon: { width: 48, height: 48, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.sm },
  actionLabel: { fontSize: fontSize.xs, fontWeight: '600', color: colors.text, textAlign: 'center' },
  statsRow: { flexDirection: 'row', gap: spacing.md, marginBottom: spacing.xxl },
  statCard: { flex: 1, backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, alignItems: 'center', ...shadow.sm },
  statValue: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text, marginTop: spacing.sm },
  statLabel: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2, fontWeight: '500' },
  taskCount: { fontSize: fontSize.xs, color: colors.warning, fontWeight: '600' },
  taskCard: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md, backgroundColor: '#fffbeb', borderRadius: radius.md, padding: spacing.md, marginBottom: spacing.sm, borderWidth: 1, borderColor: '#fde68a' },
  taskOverdue: { backgroundColor: '#fef2f2', borderColor: '#fecaca' },
  taskTitle: { fontSize: fontSize.sm, fontWeight: '600', color: colors.text },
  taskMeta: { flexDirection: 'row', gap: spacing.sm, marginTop: 3, flexWrap: 'wrap' },
  taskStage: { fontSize: 10, backgroundColor: 'rgba(255,255,255,0.8)', paddingHorizontal: 6, paddingVertical: 2, borderRadius: radius.sm, fontWeight: '500' },
  taskDay: { fontSize: 10, color: colors.textMuted },
  taskOverdueText: { fontSize: 10, color: colors.danger, fontWeight: '700' },
  farmMiniCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.white, borderRadius: radius.md, padding: spacing.md, marginBottom: spacing.sm, ...shadow.sm },
  farmMiniIcon: { width: 36, height: 36, borderRadius: radius.sm, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center', marginRight: spacing.md },
  farmMiniName: { fontSize: fontSize.md, fontWeight: '700', color: colors.text },
  farmMiniMeta: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  farmMiniBadge: { backgroundColor: colors.primaryLight, paddingHorizontal: 8, paddingVertical: 3, borderRadius: radius.full },
  farmMiniBadgeText: { fontSize: fontSize.xs, fontWeight: '600', color: colors.primary },
  emptyFarms: { alignItems: 'center', backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xxxl, ...shadow.sm, marginBottom: spacing.xl },
  emptyTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text, marginTop: spacing.md },
  emptySub: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.xs, textAlign: 'center' },
  emptyBtn: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, backgroundColor: colors.primary, paddingHorizontal: spacing.xxl, paddingVertical: spacing.md, borderRadius: radius.lg, marginTop: spacing.lg },
  emptyBtnText: { fontSize: fontSize.md, fontWeight: '700', color: colors.white },
  featuresGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md, marginBottom: spacing.xl },
  featureCard: { width: '47%', backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, ...shadow.sm },
  featureIcon: { width: 40, height: 40, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.sm },
  featureTitle: { fontSize: fontSize.md, fontWeight: '700', color: colors.text },
  featureDesc: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  aiBanner: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, backgroundColor: colors.purpleLight, borderRadius: radius.xl, padding: spacing.xl, borderWidth: 1, borderColor: '#ddd6fe', marginBottom: spacing.lg },
  aiBannerIcon: { width: 48, height: 48, borderRadius: radius.lg, backgroundColor: colors.purple, alignItems: 'center', justifyContent: 'center' },
  aiBannerTitle: { fontSize: fontSize.md, fontWeight: '700', color: '#5b21b6' },
  aiBannerSub: { fontSize: fontSize.xs, color: '#7c3aed', marginTop: 2 },
});
