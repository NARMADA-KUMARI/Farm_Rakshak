import { View, Text, ScrollView, StyleSheet, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useEffect } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { notificationApi } from '../../src/services/api';

const typeIcons: Record<string, { icon: string; color: string; bg: string }> = {
  DISEASE: { icon: 'bug-outline', color: '#ef4444', bg: '#fef2f2' },
  WEATHER: { icon: 'rainy-outline', color: '#3b82f6', bg: '#eff6ff' },
  ADVISORY: { icon: 'leaf-outline', color: '#10b981', bg: '#ecfdf5' },
  SYSTEM: { icon: 'notifications-outline', color: '#8b5cf6', bg: '#f5f3ff' },
};

export default function NotificationsScreen() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);

  const fetchNotifications = async () => {
    try {
      const res = await notificationApi.list();
      const data = res.data?.data;
      setNotifications(data?.content || data || []);
    } catch (e) { console.log('Notifications error:', e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchNotifications(); }, []);
  const onRefresh = () => { setRefreshing(true); fetchNotifications().then(() => setRefreshing(false)); };

  const markRead = async (id: string) => {
    try {
      await notificationApi.markRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    } catch (e) { console.log('Mark read error:', e); }
  };

  const markAllRead = async () => {
    setMarkingAll(true);
    try {
      await notificationApi.markAllRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (e) { console.log('Mark all read error:', e); }
    finally { setMarkingAll(false); }
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView style={s.container} showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>

        {/* Header */}
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Ionicons name="arrow-back" size={22} color={colors.text} />
          </TouchableOpacity>
          <View style={{ flex: 1 }}>
            <Text style={s.headerTitle}>Notifications</Text>
            <Text style={s.headerSub}>
              {loading ? 'Loading...' : `${notifications.length} total · ${unreadCount} unread`}
            </Text>
          </View>
          {unreadCount > 0 && (
            <TouchableOpacity onPress={markAllRead} disabled={markingAll} style={s.markAllBtn}>
              {markingAll ? (
                <ActivityIndicator size="small" color={colors.primary} />
              ) : (
                <>
                  <Ionicons name="checkmark-done" size={16} color={colors.primary} />
                  <Text style={s.markAllText}>Read all</Text>
                </>
              )}
            </TouchableOpacity>
          )}
        </View>

        {loading ? (
          <View style={s.loadingWrap}>
            <ActivityIndicator size="large" color={colors.primary} />
            <Text style={s.loadingText}>Loading notifications...</Text>
          </View>
        ) : notifications.length === 0 ? (
          <View style={s.emptyState}>
            <View style={s.emptyIcon}>
              <Ionicons name="mail-open-outline" size={48} color={colors.border} />
            </View>
            <Text style={s.emptyTitle}>No notifications yet</Text>
            <Text style={s.emptySub}>Weather alerts, disease warnings, and farming tips will appear here.</Text>
          </View>
        ) : (
          <View style={s.list}>
            {notifications.map((n) => {
              const config = typeIcons[n.type] || typeIcons.SYSTEM;
              return (
                <TouchableOpacity
                  key={n.id}
                  style={[s.card, !n.isRead && s.cardUnread]}
                  activeOpacity={0.7}
                  onPress={() => !n.isRead && markRead(n.id)}
                >
                  <View style={[s.iconBox, { backgroundColor: config.bg }]}>
                    <Ionicons name={config.icon as any} size={20} color={config.color} />
                  </View>
                  <View style={s.cardContent}>
                    <View style={s.cardTopRow}>
                      <Text style={[s.cardTitle, !n.isRead && s.cardTitleBold]} numberOfLines={1}>{n.title}</Text>
                      {!n.isRead && <View style={s.unreadDot} />}
                    </View>
                    <Text style={s.cardBody} numberOfLines={2}>{n.body}</Text>
                    <Text style={s.cardTime}>
                      {new Date(n.createdAt).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            })}
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
  header: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginTop: spacing.md, marginBottom: spacing.xl },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  headerTitle: { fontSize: fontSize.xl, fontWeight: '700', color: colors.text },
  headerSub: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: 2 },
  markAllBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: colors.primaryLight, paddingHorizontal: spacing.md, paddingVertical: spacing.sm, borderRadius: radius.full },
  markAllText: { fontSize: fontSize.xs, fontWeight: '600', color: colors.primary },
  loadingWrap: { alignItems: 'center', paddingVertical: 80 },
  loadingText: { color: colors.textMuted, marginTop: 12, fontSize: fontSize.sm },
  emptyState: { alignItems: 'center', paddingVertical: 60 },
  emptyIcon: { width: 96, height: 96, borderRadius: 48, backgroundColor: colors.borderLight, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.xl },
  emptyTitle: { fontSize: fontSize.lg, fontWeight: '700', color: colors.text },
  emptySub: { fontSize: fontSize.md, color: colors.textMuted, textAlign: 'center', marginTop: spacing.sm, paddingHorizontal: spacing.xxl },
  list: { gap: spacing.sm },
  card: { flexDirection: 'row', gap: spacing.md, backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.lg, ...shadow.sm },
  cardUnread: { backgroundColor: '#f0fdf4', borderWidth: 1, borderColor: '#bbf7d0' },
  iconBox: { width: 44, height: 44, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  cardContent: { flex: 1 },
  cardTopRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  cardTitle: { fontSize: fontSize.md, fontWeight: '500', color: colors.text, flex: 1 },
  cardTitleBold: { fontWeight: '700' },
  unreadDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.primary },
  cardBody: { fontSize: fontSize.sm, color: colors.textSecondary, marginTop: 2, lineHeight: 18 },
  cardTime: { fontSize: fontSize.xs, color: colors.textMuted, marginTop: spacing.sm },
});
