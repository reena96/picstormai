// Jest setup file for React Native Web testing

// Fix for react-native-web + @testing-library/react-native compatibility issue
// This prevents "parentInstance.children.indexOf is not a function" error
if (typeof global.document !== 'undefined') {
  const originalCreateElement = document.createElement.bind(document);
  document.createElement = function (tagName, options) {
    const element = originalCreateElement(tagName, options);
    if (!element.children || typeof element.children === 'string') {
      Object.defineProperty(element, 'children', {
        get: function () {
          return Array.from(this.childNodes).filter(node => node.nodeType === 1);
        },
        configurable: true,
      });
    }
    return element;
  };
}

// Mock lucide-react-native icons
jest.mock('lucide-react-native', () => {
  const React = require('react');
  return new Proxy({}, {
    get: (target, prop) => {
      return React.forwardRef((props, ref) =>
        React.createElement('svg', { ...props, ref, 'data-testid': prop })
      );
    }
  });
});

// Mock React Native's Animated API
global.requestAnimationFrame = (cb) => {
  setTimeout(cb, 0);
};

// Mock React Native scheduler
if (typeof window !== 'undefined') {
  Object.defineProperty(window, 'scheduler', {
    get() {
      return {
        unstable_now: () => Date.now(),
        unstable_IdlePriority: 5,
        unstable_ImmediatePriority: 1,
        unstable_LowPriority: 4,
        unstable_NormalPriority: 3,
        unstable_UserBlockingPriority: 2,
        unstable_scheduleCallback: (priority, callback) => {
          return setTimeout(callback, 0);
        },
        unstable_cancelCallback: (callbackId) => {
          clearTimeout(callbackId);
        },
        unstable_shouldYield: () => false,
      };
    },
  });
}
