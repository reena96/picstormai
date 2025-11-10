/**
 * PhotoGrid Component (Organism)
 * Responsive grid layout: 2 (mobile), 3 (tablet), 4 (desktop), 5 (large) columns
 * Features: lazy loading, infinite scroll, skeleton placeholders, empty state
 */

import React, { useState, useCallback } from 'react';
import { View, FlatList, StyleSheet, ViewStyle, Dimensions, Platform } from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import { PhotoCard, PhotoCardProps } from '../molecules/PhotoCard';
import { EmptyState } from '../molecules/EmptyState';
import { Spinner } from '../atoms/Spinner';
import { Image as ImageIcon } from 'lucide-react-native';

export interface PhotoGridProps {
  photos: PhotoCardProps[];
  onPhotoPress: (index: number) => void;
  loading?: boolean;
  onLoadMore?: () => void;
  hasMore?: boolean;
  emptyMessage?: string;
  testID?: string;
  style?: ViewStyle;
}

export const PhotoGrid: React.FC<PhotoGridProps> = ({
  photos,
  onPhotoPress,
  loading = false,
  onLoadMore,
  hasMore = false,
  emptyMessage = 'No photos yet',
  testID,
  style,
}) => {
  const { theme } = useTheme();
  const [containerWidth, setContainerWidth] = useState(Dimensions.get('window').width);

  const getNumColumns = () => {
    if (Platform.OS === 'web') {
      if (containerWidth >= 1400) return 5;
      if (containerWidth >= 1024) return 4;
      if (containerWidth >= 768) return 3;
    }
    return 2;
  };

  const numColumns = getNumColumns();
  const gap = Platform.OS === 'web' ? theme.spacing[4] : theme.spacing[2];

  const containerStyle: ViewStyle = {
    flex: 1,
    backgroundColor: theme.colors.background,
  };

  const gridStyle: ViewStyle = {
    padding: gap,
  };

  const handleLayout = (event: any) => {
    const { width } = event.nativeEvent.layout;
    setContainerWidth(width);
  };

  const handleEndReached = useCallback(() => {
    if (onLoadMore && hasMore && !loading) {
      onLoadMore();
    }
  }, [onLoadMore, hasMore, loading]);

  const renderItem = ({ item, index }: { item: PhotoCardProps; index: number }) => {
    const itemWidth = (containerWidth - gap * (numColumns + 1)) / numColumns;

    return (
      <View style={{ width: itemWidth, padding: gap / 2 }}>
        <PhotoCard
          {...item}
          onPress={() => onPhotoPress(index)}
          testID={`photo-card-${index}`}
        />
      </View>
    );
  };

  const renderSkeletons = () => {
    return Array.from({ length: 6 }).map((_, index) => (
      <View key={`skeleton-${index}`} style={{ width: (containerWidth - gap * (numColumns + 1)) / numColumns, padding: gap / 2 }}>
        <View
          style={{
            aspectRatio: 1,
            backgroundColor: theme.colors.gray[200],
            borderRadius: theme.borderRadius.base,
          }}
        />
      </View>
    ));
  };

  const renderFooter = () => {
    if (!loading || photos.length === 0) return null;

    return (
      <View style={{ padding: theme.spacing[6], alignItems: 'center' }}>
        <Spinner size="medium" />
      </View>
    );
  };

  const renderEmpty = () => {
    if (loading && photos.length === 0) {
      return (
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', padding: gap }}>
          {renderSkeletons()}
        </View>
      );
    }

    return (
      <EmptyState
        icon={ImageIcon}
        headline="No Photos"
        subtext={emptyMessage}
      />
    );
  };

  return (
    <View style={[containerStyle, style]} onLayout={handleLayout} testID={testID}>
      <FlatList
        data={photos}
        renderItem={renderItem}
        keyExtractor={(item, index) => item.photoUrl || `photo-${index}`}
        numColumns={numColumns}
        key={`grid-${numColumns}`} // Force re-render when columns change
        contentContainerStyle={photos.length === 0 ? { flex: 1 } : gridStyle}
        ListEmptyComponent={renderEmpty}
        ListFooterComponent={renderFooter}
        onEndReached={handleEndReached}
        onEndReachedThreshold={0.8}
        initialNumToRender={20}
        maxToRenderPerBatch={10}
        windowSize={5}
        removeClippedSubviews={Platform.OS === 'android'}
      />
    </View>
  );
};
