import { View, Text, StyleSheet, TouchableOpacity, TextInput, FlatList, KeyboardAvoidingView, Platform, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState, useRef } from 'react';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { aiChatApi } from '../../src/services/api';

type Message = { id: string; role: 'user' | 'ai'; text: string; time: Date };

const quickPrompts = [
  '🌾 Best crops for this season?',
  '🐛 How to prevent pest attacks?',
  '💧 Irrigation tips for wheat',
  '🌡️ Weather effects on my crop',
  '💰 When to sell my produce?',
  '🧪 Soil health improvement',
];

export default function AiChatScreen() {
  const router = useRouter();
  const [messages, setMessages] = useState<Message[]>([
    { id: '0', role: 'ai', text: 'Namaste! 🙏 I\'m your AI farming assistant. Ask me anything about crops, pests, weather, soil, or market prices. I\'m here to help!', time: new Date() },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const flatRef = useRef<FlatList>(null);

  const sendMessage = async (text: string) => {
    if (!text.trim() || loading) return;
    const userMsg: Message = { id: Date.now().toString(), role: 'user', text: text.trim(), time: new Date() };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await aiChatApi.send(text.trim());
      const reply = res.data?.data?.response || res.data?.data?.reply || res.data?.response || 'I couldn\'t process that. Please try again.';
      const aiMsg: Message = { id: (Date.now() + 1).toString(), role: 'ai', text: reply, time: new Date() };
      setMessages(prev => [...prev, aiMsg]);
    } catch (e: any) {
      const errMsg: Message = {
        id: (Date.now() + 1).toString(), role: 'ai',
        text: 'Sorry, I\'m having trouble connecting right now. Please check your internet and try again.', time: new Date(),
      };
      setMessages(prev => [...prev, errMsg]);
    } finally { setLoading(false); }

    setTimeout(() => flatRef.current?.scrollToEnd({ animated: true }), 200);
  };

  const renderMessage = ({ item }: { item: Message }) => (
    <View style={[s.msgRow, item.role === 'user' && s.msgRowUser]}>
      {item.role === 'ai' && (
        <View style={s.aiAvatar}>
          <Ionicons name="sparkles" size={16} color={colors.white} />
        </View>
      )}
      <View style={[s.msgBubble, item.role === 'user' ? s.userBubble : s.aiBubble]}>
        <Text style={[s.msgText, item.role === 'user' && s.userMsgText]}>{item.text}</Text>
        <Text style={[s.msgTime, item.role === 'user' && s.userMsgTime]}>
          {item.time.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
        </Text>
      </View>
    </View>
  );

  const showQuickPrompts = messages.length <= 1;

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Ionicons name="arrow-back" size={22} color={colors.text} />
        </TouchableOpacity>
        <View style={s.headerInfo}>
          <View style={s.headerAvatar}>
            <Ionicons name="sparkles" size={18} color={colors.white} />
          </View>
          <View>
            <Text style={s.headerTitle}>AI Assistant</Text>
            <Text style={s.headerSub}>{loading ? 'Thinking...' : 'Online · Ask anything'}</Text>
          </View>
        </View>
      </View>

      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <FlatList
          ref={flatRef}
          data={messages}
          renderItem={renderMessage}
          keyExtractor={item => item.id}
          contentContainerStyle={s.chatContainer}
          showsVerticalScrollIndicator={false}
          onContentSizeChange={() => flatRef.current?.scrollToEnd({ animated: true })}
          ListFooterComponent={
            <>
              {loading && (
                <View style={s.typingRow}>
                  <View style={s.aiAvatar}><Ionicons name="sparkles" size={16} color={colors.white} /></View>
                  <View style={s.typingBubble}>
                    <ActivityIndicator size="small" color={colors.primary} />
                    <Text style={s.typingText}>Thinking...</Text>
                  </View>
                </View>
              )}
              {showQuickPrompts && (
                <View style={s.quickSection}>
                  <Text style={s.quickLabel}>Quick questions:</Text>
                  <View style={s.quickGrid}>
                    {quickPrompts.map((q) => (
                      <TouchableOpacity key={q} style={s.quickBtn} activeOpacity={0.7} onPress={() => sendMessage(q)}>
                        <Text style={s.quickText}>{q}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </View>
              )}
            </>
          }
        />

        {/* Input Bar */}
        <View style={s.inputBar}>
          <TextInput
            style={s.input}
            placeholder="Ask about farming..."
            placeholderTextColor={colors.textMuted}
            value={input}
            onChangeText={setInput}
            multiline
            maxLength={500}
            editable={!loading}
            onSubmitEditing={() => sendMessage(input)}
          />
          <TouchableOpacity
            style={[s.sendBtn, (!input.trim() || loading) && s.sendBtnDisabled]}
            onPress={() => sendMessage(input)}
            disabled={!input.trim() || loading}
            activeOpacity={0.7}
          >
            <Ionicons name="send" size={20} color={colors.white} />
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingHorizontal: spacing.xl, paddingVertical: spacing.md, backgroundColor: colors.white, borderBottomWidth: 1, borderBottomColor: colors.borderLight },
  backBtn: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.borderLight, alignItems: 'center', justifyContent: 'center' },
  headerInfo: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, flex: 1 },
  headerAvatar: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  headerTitle: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  headerSub: { fontSize: fontSize.xs, color: colors.textMuted },
  chatContainer: { paddingHorizontal: spacing.xl, paddingTop: spacing.lg, paddingBottom: spacing.md },
  msgRow: { flexDirection: 'row', marginBottom: spacing.md, gap: spacing.sm, alignItems: 'flex-end' },
  msgRowUser: { justifyContent: 'flex-end' },
  aiAvatar: { width: 32, height: 32, borderRadius: 16, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  msgBubble: { maxWidth: '78%', padding: spacing.md, borderRadius: radius.lg },
  userBubble: { backgroundColor: colors.primary, borderBottomRightRadius: 4 },
  aiBubble: { backgroundColor: colors.white, borderBottomLeftRadius: 4, ...shadow.sm },
  msgText: { fontSize: fontSize.md, color: colors.text, lineHeight: 22 },
  userMsgText: { color: colors.white },
  msgTime: { fontSize: 9, color: colors.textMuted, marginTop: spacing.xs, textAlign: 'right' },
  userMsgTime: { color: 'rgba(255,255,255,0.6)' },
  typingRow: { flexDirection: 'row', gap: spacing.sm, alignItems: 'flex-end', marginBottom: spacing.md },
  typingBubble: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, backgroundColor: colors.white, padding: spacing.md, borderRadius: radius.lg, ...shadow.sm },
  typingText: { fontSize: fontSize.sm, color: colors.textMuted },
  quickSection: { marginTop: spacing.xl },
  quickLabel: { fontSize: fontSize.sm, fontWeight: '600', color: colors.textSecondary, marginBottom: spacing.md },
  quickGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  quickBtn: { backgroundColor: colors.white, paddingHorizontal: spacing.md, paddingVertical: spacing.sm + 2, borderRadius: radius.full, borderWidth: 1, borderColor: colors.border, ...shadow.sm },
  quickText: { fontSize: fontSize.sm, color: colors.text },
  inputBar: { flexDirection: 'row', alignItems: 'flex-end', gap: spacing.sm, paddingHorizontal: spacing.xl, paddingVertical: spacing.md, backgroundColor: colors.white, borderTopWidth: 1, borderTopColor: colors.borderLight },
  input: { flex: 1, backgroundColor: colors.borderLight, borderRadius: radius.lg, paddingHorizontal: spacing.lg, paddingVertical: spacing.md, fontSize: fontSize.md, color: colors.text, maxHeight: 100, borderWidth: 1, borderColor: colors.border },
  sendBtn: { width: 48, height: 48, borderRadius: radius.lg, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', ...shadow.sm },
  sendBtnDisabled: { opacity: 0.4 },
});
