// Mock Modal component for testing
// Prevents react-native-web Modal rendering issues with test renderer

const React = require('react');

const Modal = ({ children, visible, ...props }) => {
  if (!visible) return null;
  return React.createElement('div', { 'data-testid': 'modal', ...props }, children);
};

module.exports = Modal;
