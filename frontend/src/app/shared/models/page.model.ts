export interface PageMetadata {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface RawPageResponse<T> {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
  first?: boolean;
  last?: boolean;
  page?: Partial<PageMetadata>;
}

export function normalizePage<T>(response: RawPageResponse<T>): Page<T> {
  const metadata = response.page ?? {};
  const size = response.size ?? metadata.size ?? 0;
  const number = response.number ?? metadata.number ?? 0;
  const totalElements = response.totalElements ?? metadata.totalElements ?? 0;
  const totalPages = response.totalPages ?? metadata.totalPages ?? 0;

  return {
    content: response.content ?? [],
    totalElements,
    totalPages,
    size,
    number,
    first: response.first ?? number <= 0,
    last: response.last ?? (totalPages === 0 || number >= totalPages - 1)
  };
}
