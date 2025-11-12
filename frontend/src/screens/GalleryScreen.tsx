/**
 * Gallery Screen - Photo Gallery with Infinite Scroll
 * Story 3.1: Photo Gallery UI with Infinite Scroll
 */

import React, { useState, useEffect } from 'react';
import { View, FlatList, StyleSheet, Text } from 'react-native';
import { useTheme } from '../hooks/useTheme';
import axios from 'axios';

interface Photo {
  id: string;
  url: string;
  filename: string;
  createdAt: string;
}

export const GalleryScreen: React.FC = () => {
  const { theme } = useTheme();
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const loadPhotos = async (pageNum: number) => {
    setLoading(true);
    try {
      const response = await axios.get(
        `/api/photos?page=${pageNum}&size=30&sort=createdAt,desc`
      );
      setPhotos(prev => [...prev, ...response.data.content]);
    } catch (error) {
      console.error('Failed to load photos', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPhotos(0);
  }, []);

  const handleLoadMore = () => {
    if (!loading) {
      loadPhotos(page + 1);
      setPage(page + 1);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Photo Gallery</Text>
      <FlatList
        data={photos}
        numColumns={3}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.5}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.photoCard}>
            <Text>{item.filename}</Text>
          </View>
        )}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16 },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 16 },
  photoCard: { flex: 1, margin: 4, height: 150 },
});

export default GalleryScreen;
