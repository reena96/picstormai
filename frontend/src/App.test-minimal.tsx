/**
 * Minimal test App to verify React Native Web is working
 */

import React from 'react';
import { View, Text } from 'react-native';

const App: React.FC = () => {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f0f0f0' }}>
      <Text style={{ fontSize: 24, fontWeight: 'bold', color: '#333' }}>
        PicStormAI - Frontend Running ✅
      </Text>
      <Text style={{ fontSize: 16, color: '#666', marginTop: 10 }}>
        If you see this, React Native Web is working!
      </Text>
    </View>
  );
};

export default App;
