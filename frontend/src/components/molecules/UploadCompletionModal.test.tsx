/**
 * UploadCompletionModal Tests
 * Story 2.9: Upload Completion Notification
 * Tests modal rendering, states, auto-dismiss, and accessibility
 */

import React from 'react';
import { render as renderDOM } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { UploadCompletionModal } from './UploadCompletionModal';

expect.extend(toHaveNoViolations);

// Mock canvas-confetti for web
jest.mock('canvas-confetti', () => ({
  __esModule: true,
  default: jest.fn(),
}));

describe('UploadCompletionModal', () => {
  const mockOnViewPhotos = jest.fn();
  const mockOnRetryFailed = jest.fn();
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  describe('Modal Props and Interface', () => {
    it('accepts all required props', () => {
      const props = {
        visible: true,
        totalPhotos: 100,
        successfulPhotos: 100,
        failedPhotos: 0,
        onViewPhotos: mockOnViewPhotos,
        onClose: mockOnClose,
      };

      expect(() => {
        renderDOM(<UploadCompletionModal {...props} />);
      }).not.toThrow();
    });

    it('accepts optional props', () => {
      const props = {
        visible: true,
        totalPhotos: 100,
        successfulPhotos: 85,
        failedPhotos: 15,
        onViewPhotos: mockOnViewPhotos,
        onRetryFailed: mockOnRetryFailed,
        onClose: mockOnClose,
        soundEnabled: false,
      };

      expect(() => {
        renderDOM(<UploadCompletionModal {...props} />);
      }).not.toThrow();
    });
  });

  describe('Full Success State (AC1)', () => {
    it('renders success modal when all photos uploaded successfully', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(getByText('Upload Complete! 🎉')).toBeTruthy();
      expect(getByText('All 100 photos uploaded successfully')).toBeTruthy();
      expect(getByText('View Photos')).toBeTruthy();
    });

    it('does not show Retry Failed button when all photos succeed', () => {
      const { queryByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(queryByText('Retry Failed')).toBeNull();
    });
  });

  describe('Partial Success State (AC2)', () => {
    it('renders partial success modal with warning state', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={85}
          failedPhotos={15}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
        />
      );

      expect(getByText('Upload Completed')).toBeTruthy();
      expect(getByText('85 of 100 photos uploaded successfully')).toBeTruthy();
      expect(getByText('15 photos failed')).toBeTruthy();
    });

    it('shows both View Photos and Retry Failed buttons for partial success', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={85}
          failedPhotos={15}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
        />
      );

      expect(getByText('View Photos')).toBeTruthy();
      expect(getByText('Retry Failed')).toBeTruthy();
    });

    it('displays singular "photo" for single failure', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={99}
          failedPhotos={1}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
        />
      );

      expect(getByText('1 photo failed')).toBeTruthy();
    });
  });

  describe('Modal Visibility', () => {
    it('renders when visible is true', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(getByText('Upload Complete! 🎉')).toBeTruthy();
    });

    it('does not render when visible is false', () => {
      const { queryByText } = renderDOM(
        <UploadCompletionModal
          visible={false}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(queryByText('Upload Complete! 🎉')).toBeNull();
    });
  });

  describe('Sound Effects (AC3)', () => {
    it('respects soundEnabled prop', () => {
      const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();

      renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
          soundEnabled={true}
        />
      );

      expect(consoleLogSpy).toHaveBeenCalledWith('Playing success sound');
      consoleLogSpy.mockRestore();
    });

    it('does not play sound when soundEnabled is false', () => {
      const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();

      renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
          soundEnabled={false}
        />
      );

      expect(consoleLogSpy).not.toHaveBeenCalledWith('Playing success sound');
      consoleLogSpy.mockRestore();
    });

    it('does not play sound when no photos succeeded', () => {
      const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();

      renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={0}
          failedPhotos={100}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
          soundEnabled={true}
        />
      );

      expect(consoleLogSpy).not.toHaveBeenCalledWith('Playing success sound');
      consoleLogSpy.mockRestore();
    });
  });

  describe('Accessibility (AC5)', () => {
    it('renders accessible button elements', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={100}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      const viewPhotosButton = getByText('View Photos');
      expect(viewPhotosButton).toBeTruthy();
      expect(viewPhotosButton.tagName).toBe('DIV'); // Pressable renders as div in web
    });

    it('provides accessible text for screen readers', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={85}
          failedPhotos={15}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
        />
      );

      // Verify informative text is present for screen readers
      expect(getByText('Upload Completed')).toBeTruthy();
      expect(getByText('85 of 100 photos uploaded successfully')).toBeTruthy();
      expect(getByText('15 photos failed')).toBeTruthy();
    });
  });

  describe('Edge Cases', () => {
    it('handles 0 total photos gracefully', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={0}
          successfulPhotos={0}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(getByText('All 0 photos uploaded successfully')).toBeTruthy();
    });

    it('handles all photos failed', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={10}
          successfulPhotos={0}
          failedPhotos={10}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
        />
      );

      expect(getByText('Upload Completed')).toBeTruthy();
      expect(getByText('0 of 10 photos uploaded successfully')).toBeTruthy();
      expect(getByText('10 photos failed')).toBeTruthy();
    });

    it('does not show Retry Failed button when onRetryFailed is undefined', () => {
      const { queryByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={100}
          successfulPhotos={85}
          failedPhotos={15}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(queryByText('Retry Failed')).toBeNull();
    });
  });

  describe('Component Behavior', () => {
    it('determines success state correctly', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={50}
          successfulPhotos={50}
          failedPhotos={0}
          onViewPhotos={mockOnViewPhotos}
          onClose={mockOnClose}
        />
      );

      expect(getByText('Upload Complete! 🎉')).toBeTruthy();
    });

    it('determines failure state correctly', () => {
      const { getByText } = renderDOM(
        <UploadCompletionModal
          visible={true}
          totalPhotos={50}
          successfulPhotos={40}
          failedPhotos={10}
          onViewPhotos={mockOnViewPhotos}
          onRetryFailed={mockOnRetryFailed}
          onClose={mockOnClose}
        />
      );

      expect(getByText('Upload Completed')).toBeTruthy();
    });
  });
});
