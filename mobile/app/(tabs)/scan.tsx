import { View, Text, StyleSheet, TouchableOpacity, Image, ActivityIndicator, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { colors, spacing, radius, fontSize, shadow } from '../../src/theme';
import { cropApi } from '../../src/services/api';

export default function ScanScreen() {
  const router = useRouter();
  const [image, setImage] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const pickImage = async (fromCamera: boolean) => {
    setError('');
    try {
      const permFn = fromCamera ? ImagePicker.requestCameraPermissionsAsync : ImagePicker.requestMediaLibraryPermissionsAsync;
      const perm = await permFn();
      if (!perm.granted) {
        Alert.alert('Permission needed', `Please allow ${fromCamera ? 'camera' : 'gallery'} access to scan crops.`);
        return;
      }
      const result = fromCamera
        ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.8 })
        : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.8 });
      if (!result.canceled && result.assets?.[0]) {
        setImage(result.assets[0].uri);
      }
    } catch (e) { setError('Failed to pick image'); }
  };

  const handleUpload = async () => {
    if (!image) return;
    setUploading(true); setError('');
    try {
      const formData = new FormData();
      const filename = image.split('/').pop() || 'photo.jpg';
      const match = /\.(\w+)$/.exec(filename);
      const type = match ? `image/${match[1]}` : 'image/jpeg';
      formData.append('image', { uri: image, name: filename, type } as any);

      const res = await cropApi.uploadImage(formData);
      const uploadId = res.data?.data?.uploadId || res.data?.uploadId;
      if (uploadId) {
        router.push({ pathname: '/scan-result', params: { uploadId } });
      } else {
        setError('Scan completed but no result ID returned');
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Upload failed. Please try again.');
    } finally { setUploading(false); }
  };

  return (
    <SafeAreaView style={s.safe} edges={['top']}>
      <View style={s.container}>
        <Text style={s.title}>Disease Scanner</Text>
        <Text style={s.sub}>AI-powered plant disease detection</Text>

        {!image ? (
          <View style={s.scanArea}>
            <View style={s.scanCircle}>
              <Ionicons name="camera-outline" size={48} color={colors.primary} />
            </View>
            <Text style={s.scanText}>Take a photo of your crop leaf</Text>
            <Text style={s.scanSub}>Our AI will detect diseases with 95% accuracy</Text>
          </View>
        ) : (
          <View style={s.previewWrap}>
            <Image source={{ uri: image }} style={s.preview} resizeMode="cover" />
            <TouchableOpacity style={s.removeBtn} onPress={() => { setImage(null); setError(''); }}>
              <Ionicons name="close-circle" size={32} color="rgba(0,0,0,0.6)" />
            </TouchableOpacity>
          </View>
        )}

        {error ? (
          <View style={s.errorBox}>
            <Ionicons name="alert-circle" size={16} color={colors.danger} />
            <Text style={s.errorText}>{error}</Text>
          </View>
        ) : null}

        {image ? (
          <TouchableOpacity style={[s.uploadBtn, uploading && s.btnDisabled]} onPress={handleUpload} disabled={uploading} activeOpacity={0.8}>
            {uploading ? (
              <>
                <ActivityIndicator color={colors.white} />
                <Text style={s.uploadBtnText}>Analyzing...</Text>
              </>
            ) : (
              <>
                <Ionicons name="sparkles" size={20} color={colors.white} />
                <Text style={s.uploadBtnText}>Analyze Image</Text>
              </>
            )}
          </TouchableOpacity>
        ) : null}

        {!image && (
          <>
            <TouchableOpacity style={s.cameraBtn} activeOpacity={0.8} onPress={() => pickImage(true)}>
              <Ionicons name="camera" size={24} color={colors.white} />
              <Text style={s.cameraBtnText}>Open Camera</Text>
            </TouchableOpacity>

            <TouchableOpacity style={s.galleryBtn} activeOpacity={0.8} onPress={() => pickImage(false)}>
              <Ionicons name="images-outline" size={22} color={colors.primary} />
              <Text style={s.galleryBtnText}>Choose from Gallery</Text>
            </TouchableOpacity>
          </>
        )}

        {/* Tips */}
        <View style={s.tipsCard}>
          <View style={s.tipsHeader}>
            <Ionicons name="information-circle" size={16} color="#2563eb" />
            <Text style={s.tipsTitle}>Photo Tips</Text>
          </View>
          <Text style={s.tipText}>• Take a clear, close-up photo of the affected leaf</Text>
          <Text style={s.tipText}>• Ensure good lighting — avoid shadows</Text>
          <Text style={s.tipText}>• Include both healthy and affected parts if possible</Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  container: { flex: 1, paddingHorizontal: spacing.xl, paddingTop: spacing.xl },
  title: { fontSize: fontSize.xxl, fontWeight: '800', color: colors.text },
  sub: { fontSize: fontSize.md, color: colors.textSecondary, marginTop: spacing.xs, marginBottom: spacing.xxxl },
  scanArea: { alignItems: 'center', paddingVertical: 48, backgroundColor: colors.white, borderRadius: radius.xl, borderWidth: 2, borderStyle: 'dashed', borderColor: colors.primaryLight, marginBottom: spacing.xxl, ...shadow.sm },
  scanCircle: { width: 96, height: 96, borderRadius: 48, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center', marginBottom: spacing.lg },
  scanText: { fontSize: fontSize.base, fontWeight: '700', color: colors.text },
  scanSub: { fontSize: fontSize.sm, color: colors.textMuted, marginTop: spacing.xs },
  previewWrap: { position: 'relative', borderRadius: radius.xl, overflow: 'hidden', marginBottom: spacing.xl, ...shadow.md },
  preview: { width: '100%', height: 250, borderRadius: radius.xl },
  removeBtn: { position: 'absolute', top: 12, right: 12 },
  errorBox: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, padding: spacing.md, backgroundColor: colors.dangerLight, borderRadius: radius.md, marginBottom: spacing.md },
  errorText: { fontSize: fontSize.sm, color: colors.danger, flex: 1 },
  uploadBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.primary, borderRadius: radius.lg, paddingVertical: spacing.lg, ...shadow.md, marginBottom: spacing.md },
  uploadBtnText: { fontSize: fontSize.base, fontWeight: '700', color: colors.white },
  btnDisabled: { opacity: 0.6 },
  cameraBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.primary, borderRadius: radius.lg, paddingVertical: spacing.lg, ...shadow.md },
  cameraBtnText: { fontSize: fontSize.base, fontWeight: '700', color: colors.white },
  galleryBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: spacing.sm, backgroundColor: colors.white, borderRadius: radius.lg, paddingVertical: spacing.lg, marginTop: spacing.md, borderWidth: 1, borderColor: colors.border },
  galleryBtnText: { fontSize: fontSize.base, fontWeight: '600', color: colors.primary },
  tipsCard: { backgroundColor: '#eff6ff', borderRadius: radius.lg, padding: spacing.lg, marginTop: spacing.xxl, borderWidth: 1, borderColor: '#bfdbfe' },
  tipsHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.sm },
  tipsTitle: { fontSize: fontSize.sm, fontWeight: '700', color: '#1e40af' },
  tipText: { fontSize: fontSize.sm, color: '#1e40af', lineHeight: 20, marginTop: 2 },
});
