export interface Category {
  id: number;
  name: string;
  description: string | null;
  productCount: number;
}

export interface CategoryRequest {
  name: string;
  description: string | null;
}
