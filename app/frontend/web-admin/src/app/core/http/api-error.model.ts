/**
 * The error envelope every KApp service returns, byte-for-byte identical across all the specs
 * (see the `ApiError` schema repeated in each docs/api/*.openapi.yaml). Parsed once here so no
 * feature re-implements "what does a failed request look like".
 */
export interface ApiErrorDetail {
  field?: string;
  issue?: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: ApiErrorDetail[];
}
