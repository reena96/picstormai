/**
 * Upload Retry Logic
 * Story 2.12: Upload Retry & Resume
 */

export class RetryManager {
  private maxRetries = 3;
  private baseDelay = 1000; // 1 second

  async retryWithBackoff<T>(
    fn: () => Promise<T>,
    attempt = 0
  ): Promise<T> {
    try {
      return await fn();
    } catch (error) {
      if (attempt >= this.maxRetries) {
        throw error;
      }

      const delay = this.baseDelay * Math.pow(2, attempt);
      await new Promise(resolve => setTimeout(resolve, delay));

      return this.retryWithBackoff(fn, attempt + 1);
    }
  }
}

export const retryManager = new RetryManager();
