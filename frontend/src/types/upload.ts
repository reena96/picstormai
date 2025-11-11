/**
 * Upload-related type definitions
 */

export interface SelectedPhoto {
  id: string;
  uri: string;
  name: string;
  type: string;
  size: number;
  thumbnail?: string;
}

export interface UploadValidationError {
  photo: SelectedPhoto;
  error: string;
}

export const MAX_PHOTOS_PER_UPLOAD = 100;
export const MAX_FILE_SIZE_MB = 50;
export const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

export const SUPPORTED_IMAGE_TYPES = [
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/gif',
  'image/webp',
];

export function validatePhoto(photo: SelectedPhoto): string | null {
  // Check file type
  if (!SUPPORTED_IMAGE_TYPES.includes(photo.type.toLowerCase())) {
    return 'Only image files allowed (JPG, PNG, GIF, WebP)';
  }

  // Check file size
  if (photo.size > MAX_FILE_SIZE_BYTES) {
    return `File size exceeds ${MAX_FILE_SIZE_MB}MB limit`;
  }

  return null;
}

export function validatePhotoCount(count: number): string | null {
  if (count > MAX_PHOTOS_PER_UPLOAD) {
    return `Maximum ${MAX_PHOTOS_PER_UPLOAD} photos per upload`;
  }
  return null;
}
