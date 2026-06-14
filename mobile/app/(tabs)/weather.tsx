import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { weatherApi } from '../../src/services/api';

const weatherIcons: Record<string, string> = {
  thunderstorm: 'thunderstorm-outline',
  drizzle: 'rainy-outline',
  rain: 'rainy-outline',
  snow: 'snow-outline',
  clouds: 'cloudy-outline',
  mist: 'cloud-outline',
  haze: 'cloud-outline',
  clear: 'sunny-outline',
};

const weatherGradients: Record<string, string[]> = {
  thunderstorm: ['#374151', '#1f2937'],
  rain: ['#2563eb', '#475569'],
  clouds: ['#6b7280', '#3b82f6'],
  clear: ['#3b82f6', '#06b6d4'],
};

function formatTime(ts: number) {
  if (!ts) return '—';
  return new Date(ts * 1000).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
}

function windDirection(deg: number) {
  const dirs = ['N','NNE','NE','ENE','E','ESE','SE','SSE','S','SSW','SW','WSW','W','WNW','NW','NNW'];
  return dirs[Math.round(deg / 22.5) % 16];
}

export default function WeatherScreen() {
  const router = useRouter();
  const [weather, setWeather] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [cityName, setCityName] = useState('Your Region');

  useEffect(() => {
    const load = async () => {
      try {
        // Try default weather first (backend handles coords)
        const res = await weatherApi.getCurrent();
        const w = res.data?.data || res.data;
        setWeather(w);
        if (w?.cityName && w.cityName !== 'Unknown') {
          setCityName(w.cityName + (w.country ? `, ${w.country}` : ''));
        }
      } catch (e) { console.log('Weather error:', e); }
      finally { setLoading(false); }
    };
    load();
  }, []);

  const iconName = weather?.mainWeather
    ? (weatherIcons[weather.mainWeather.toLowerCase()] || 'sunny-outline')
    : 'sunny-outline';

  if (loading) {
    return (
      <SafeAreaView style={[s.safe, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={colors.primary} />
        <Text style={s.loadingText}>Loading weather...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}>
        {/* Back Header */}
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Ionicons name="arrow-back" size={22} color={colors.text} />
          </TouchableOpacity>
          <Text style={s.headerTitle}>Weather</Text>
          <View style={{ width: 40 }} />
        </View>

        {weather ? (
          <>
            {/* Hero Card */}
            <View style={s.heroCard}>
              <View style={s.heroCircle} />
              <View style={s.heroContent}>
                <View>
                  <View style={s.locationRow}>
                    <Ionicons name="location" size={14} color="rgba(255,255,255,0.7)" />
                    <Text style={s.locationText}>{cityName}</Text>
                  </View>
                  <Text style={s.heroTemp}>{weather.temperature}°C</Text>
                  <Text style={s.heroDesc}>{weather.description}</Text>
                  <View style={s.heroRange}>
                    <View style={s.rangeItem}>
                      <Ionicons name="arrow-up" size={12} color="rgba(255,255,255,0.6)" />
                      <Text style={s.rangeText}>{weather.tempMax}°</Text>
                    </View>
                    <View style={s.rangeItem}>
                      <Ionicons name="arrow-down" size={12} color="rgba(255,255,255,0.6)" />
                      <Text style={s.rangeText}>{weather.tempMin}°</Text>
                    </View>
                    <Text style={s.feelsLike}>Feels like {weather.feelsLike}°C</Text>
                  </View>
                </View>
                <Ionicons name={iconName as any} size={72} color="rgba(255,255,255,0.3)" />
              </View>
            </View>

            {/* Detail Cards */}
            <View style={s.detailGrid}>
              {[
                { icon: 'water-outline', label: 'Humidity', value: `${weather.humidity}%`, color: '#3b82f6', bg: '#eff6ff',
                  sub: weather.humidity > 80 ? '⚠️ High' : weather.humidity < 30 ? '⚠️ Low' : 'Normal' },
                { icon: 'speedometer-outline', label: 'Wind Speed', value: `${weather.windSpeed} km/h`, color: '#64748b', bg: '#f8fafc',
                  sub: weather.windDeg ? windDirection(weather.windDeg) : '' },
                { icon: 'cloudy-outline', label: 'Cloud Cover', value: `${weather.cloudCover || 0}%`, color: '#6b7280', bg: '#f9fafb',
                  sub: (weather.cloudCover || 0) > 80 ? 'Overcast' : 'Partly clear' },
                { icon: 'rainy-outline', label: 'Rain', value: weather.rainVolume > 0 ? `${weather.rainVolume} mm` : weather.rainForecast ? 'Expected' : 'None', color: '#06b6d4', bg: '#ecfeff',
                  sub: weather.rainForecast ? '🌧️ Rain likely' : 'No rain' },
              ].map((item) => (
                <View key={item.label} style={s.detailCard}>
                  <View style={[s.detailIcon, { backgroundColor: item.bg }]}>
                    <Ionicons name={item.icon as any} size={22} color={item.color} />
                  </View>
                  <Text style={s.detailValue}>{item.value}</Text>
                  <Text style={s.detailLabel}>{item.label}</Text>
                  <Text style={s.detailSub}>{item.sub}</Text>
                </View>
              ))}
            </View>

            {/* Extra Info */}
            <View style={s.extraGrid}>
              {[
                { icon: 'speedometer', label: 'Pressure', value: `${weather.pressure} hPa`, color: '#7c3aed', bg: '#f5f3ff' },
                { icon: 'eye-outline', label: 'Visibility', value: `${weather.visibility} km`, color: '#4f46e5', bg: '#eef2ff' },
                { icon: 'sunny-outline', label: 'Sunrise', value: formatTime(weather.sunrise), color: '#d97706', bg: '#fffbeb' },
                { icon: 'moon-outline', label: 'Sunset', value: formatTime(weather.sunset), color: '#ea580c', bg: '#fff7ed' },
              ].map((item) => (
                <View key={item.label} style={s.extraCard}>
                  <View style={[s.extraIcon, { backgroundColor: item.bg }]}>
                    <Ionicons name={item.icon as any} size={18} color={item.color} />
                  </View>
                  <View>
                    <Text style={s.extraValue}>{item.value}</Text>
                    <Text style={s.extraLabel}>{item.label}</Text>
                  </View>
                </View>
              ))}
            </View>

            {/* Farming Alerts */}
            <View style={s.alertsCard}>
              <View style={s.alertsHeader}>
                <Ionicons name="warning-outline" size={18} color={colors.warning} />
                <Text style={s.alertsTitle}>Farming Alerts</Text>
              </View>
              {weather.alerts && weather.alerts.length > 0 ? (
                weather.alerts.map((alert: string, i: number) => (
                  <View key={i} style={s.alertItem}>
                    <Ionicons name="alert-circle" size={16} color={colors.warning} />
                    <Text style={s.alertText}>{alert}</Text>
                  </View>
                ))
              ) : (
                <View style={s.noAlerts}>
                  <Ionicons name="sunny" size={32} color={colors.warningLight} />
                  <Text style={s.noAlertText}>No farming alerts ✅</Text>
                </View>
              )}
            </View>
          </>
        ) : (
          <View style={s.emptyState}>
            <Ionicons name="cloud-outline" size={64} color={colors.border} />
            <Text style={s.emptyText}>Weather data unavailable</Text>
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
  heroCard: { backgroundColor: '#3b82f6', borderRadius: radius.xl, padding: spacing.xxl, marginBottom: spacing.xl, overflow: 'hidden', ...shadow.lg },
  heroCircle: { position: 'absolute', top: -40, right: -40, width: 160, height: 160, borderRadius: 80, backgroundColor: 'rgba(255,255,255,0.08)' },
  heroContent: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  locationRow: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: spacing.sm },
  locationText: { fontSize: fontSize.sm, color: 'rgba(255,255,255,0.7)' },
  heroTemp: { fontSize: 52, fontWeight: '800', color: '#ffffff' },
  heroDesc: { fontSize: fontSize.base, color: 'rgba(255,255,255,0.8)', textTransform: 'capitalize' },
  heroRange: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginTop: spacing.sm },
  rangeItem: { flexDirection: 'row', alignItems: 'center', gap: 2 },
  rangeText: { fontSize: fontSize.sm, color: 'rgba(255,255,255,0.6)' },
  feelsLike: { fontSize: fontSize.sm, color: 'rgba(255,255,255,0.6)' },
  detailGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md, marginBottom: spacing.xl },
  detailCard: { width: '47%', backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, alignItems: 'center', ...shadow.sm },
  detailIcon: { width: 48, height: 48, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.sm },
  detailValue: { fontSize: fontSize.xl, fontWeight: '800', color: colors.text },
  detailLabel: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  detailSub: { fontSize: 9, color: colors.textMuted, marginTop: 2 },
  extraGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md, marginBottom: spacing.xl },
  extraCard: { width: '47%', flexDirection: 'row', alignItems: 'center', gap: spacing.md, backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, ...shadow.sm },
  extraIcon: { width: 40, height: 40, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  extraValue: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  extraLabel: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 1 },
  alertsCard: { backgroundColor: colors.white, borderRadius: radius.xl, padding: spacing.xl, ...shadow.sm },
  alertsHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.lg },
  alertsTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text },
  alertItem: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.md, padding: spacing.md, backgroundColor: colors.warningLight, borderRadius: radius.md, marginBottom: spacing.sm },
  alertText: { flex: 1, fontSize: fontSize.sm, color: '#92400e' },
  noAlerts: { alignItems: 'center', paddingVertical: spacing.xxl },
  noAlertText: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.sm },
  emptyState: { alignItems: 'center', paddingVertical: 80 },
  emptyText: { fontSize: fontSize.md, color: colors.textMuted, marginTop: spacing.md },
});
