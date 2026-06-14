import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors, fontSize } from '../../src/theme';
import { Platform } from 'react-native';

export default function TabsLayout() {
  return (
    <Tabs screenOptions={{
      headerShown: false,
      tabBarActiveTintColor: colors.primary,
      tabBarInactiveTintColor: colors.textMuted,
      tabBarLabelStyle: { fontSize: 9, fontWeight: '600', marginBottom: Platform.OS === 'ios' ? 0 : 4 },
      tabBarIconStyle: { marginTop: 4 },
      tabBarStyle: {
        height: Platform.OS === 'ios' ? 84 : 60,
        paddingTop: 4,
        paddingBottom: Platform.OS === 'ios' ? 20 : 6,
        borderTopWidth: 1,
        borderTopColor: colors.borderLight,
        backgroundColor: colors.white,
        elevation: 8,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: -2 },
        shadowOpacity: 0.06,
        shadowRadius: 8,
      },
    }}>
      {/* ─── 7 Visible Tabs ─── */}
      <Tabs.Screen name="home" options={{
        title: 'Home',
        tabBarIcon: ({ color, size }) => <Ionicons name="home" size={20} color={color} />,
      }} />
      <Tabs.Screen name="farms" options={{
        title: 'Farm',
        tabBarIcon: ({ color, size }) => <Ionicons name="business" size={20} color={color} />,
      }} />
      <Tabs.Screen name="crops" options={{
        title: 'Crop',
        tabBarIcon: ({ color, size }) => <Ionicons name="leaf" size={20} color={color} />,
      }} />
      <Tabs.Screen name="market" options={{
        title: 'Market',
        tabBarIcon: ({ color, size }) => <Ionicons name="trending-up" size={20} color={color} />,
      }} />
      <Tabs.Screen name="ai-chat" options={{
        title: 'AI',
        tabBarIcon: ({ color, size }) => <Ionicons name="sparkles" size={20} color={color} />,
      }} />
      <Tabs.Screen name="scan" options={{
        title: 'Scan',
        tabBarIcon: ({ color, size }) => <Ionicons name="camera" size={20} color={color} />,
      }} />
      <Tabs.Screen name="profile" options={{
        title: 'Profile',
        tabBarIcon: ({ color, size }) => <Ionicons name="person" size={20} color={color} />,
      }} />

      {/* ─── Hidden screens (tab bar stays visible) ─── */}
      <Tabs.Screen name="weather" options={{ href: null }} />
      <Tabs.Screen name="notifications" options={{ href: null }} />
      <Tabs.Screen name="advisory" options={{ href: null }} />
      <Tabs.Screen name="farm-detail" options={{ href: null }} />
      <Tabs.Screen name="add-farm" options={{ href: null }} />
      <Tabs.Screen name="scan-result" options={{ href: null }} />
      <Tabs.Screen name="crop-detail" options={{ href: null }} />
    </Tabs>
  );
}
