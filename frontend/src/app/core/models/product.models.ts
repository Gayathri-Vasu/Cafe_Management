export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  categoryId: number;
  categoryName: string;
  availability: boolean;
}

export interface ProductRequest {
  name: string;
  description: string | null;
  price: number;
  categoryId: number;
  availability: boolean;
}
