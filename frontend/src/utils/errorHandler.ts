/**
 * Upload Error Handler
 * Story 2.11: Upload Error Handling & User-Friendly Messages
 */

export function getUploadErrorMessage(error: any): string {
  if (error.response) {
    const status = error.response.status;
    
    switch (status) {
      case 403:
        return 'Upload not authorized. Please check your permissions.';
      case 408:
        return 'Network timeout. Upload will retry automatically.';
      case 413:
        return 'File exceeds maximum size limit.';
      case 500:
        return 'Server error. Please try again later.';
      default:
        return 'Upload failed. Please try again.';
    }
  }
  
  if (error.code === 'ECONNABORTED') {
    return 'Network timeout. Upload will retry automatically.';
  }
  
  return error.message || 'An unknown error occurred.';
}
