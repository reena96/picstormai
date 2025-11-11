/**
 * Lightbox Component - Fullscreen Photo Viewer
 * Story 3.2: Photo Viewing - Lightbox
 */

import React from 'react';
import { View, Image, TouchableOpacity, StyleSheet, Text, Modal } from 'react-native';
import { X, ChevronLeft, ChevronRight } from 'lucide-react-native';

interface LightboxProps {
  visible: boolean;
  photoUrl: string;
  photoName: string;
  onClose: () => void;
  onNext?: () => void;
  onPrevious?: () => void;
}

export const Lightbox: React.FC<LightboxProps> = ({
  visible,
  photoUrl,
  photoName,
  onClose,
  onNext,
  onPrevious,
}) => {
  return (
    <Modal visible={visible} transparent animationType="fade">
      <View style={styles.container}>
        <TouchableOpacity style={styles.closeButton} onPress={onClose}>
          <X size={24} color="#FFFFFF" />
        </TouchableOpacity>

        {onPrevious && (
          <TouchableOpacity style={styles.prevButton} onPress={onPrevious}>
            <ChevronLeft size={32} color="#FFFFFF" />
          </TouchableOpacity>
        )}

        <Image source={{ uri: photoUrl }} style={styles.image} resizeMode="contain" />

        {onNext && (
          <TouchableOpacity style={styles.nextButton} onPress={onNext}>
            <ChevronRight size={32} color="#FFFFFF" />
          </TouchableOpacity>
        )}

        <View style={styles.infoBar}>
          <Text style={styles.filename}>{photoName}</Text>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.9)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  image: { width: '90%', height: '80%' },
  closeButton: { position: 'absolute', top: 20, right: 20, zIndex: 10 },
  prevButton: { position: 'absolute', left: 20, zIndex: 10 },
  nextButton: { position: 'absolute', right: 20, zIndex: 10 },
  infoBar: { position: 'absolute', bottom: 20, left: 20, right: 20 },
  filename: { color: '#FFFFFF', fontSize: 16 },
});
