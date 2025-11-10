// Jest setup file for React Native Web testing

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
