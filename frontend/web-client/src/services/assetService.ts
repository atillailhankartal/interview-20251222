import api, { type ApiResponse } from './api'

// Asset types
export interface CustomerAsset {
  id: string
  customerId: string
  assetName: string
  size: number
  usableSize: number
  blockedSize: number
  averageCost?: number
  currentPrice?: number
  marketValue?: number
  profitLoss?: number
  profitLossPercent?: number
  updatedAt: string
}

export interface DepositRequest {
  customerId?: string  // Optional - backend uses JWT for CUSTOMER role
  assetName: string
  amount: number
}

export interface WithdrawRequest {
  customerId?: string  // Optional - backend uses JWT for CUSTOMER role
  assetName: string
  amount: number
}

export interface AssetStats {
  totalAssets: number
  totalMarketValue: number
  totalProfitLoss: number
  topAssets: CustomerAsset[]
}

// Asset service functions
export const assetService = {
  /**
   * Get all assets for a customer
   */
  async getCustomerAssets(customerId?: string): Promise<ApiResponse<CustomerAsset[]>> {
    const params = customerId ? `?customerId=${customerId}` : ''
    const response = await api.get<ApiResponse<CustomerAsset[]>>(`/assets${params}`)
    return response.data
  },

  /**
   * Get a specific asset for a customer
   */
  async getCustomerAsset(customerId: string, assetName: string): Promise<ApiResponse<CustomerAsset>> {
    const response = await api.get<ApiResponse<CustomerAsset>>(`/assets/${customerId}/${assetName}`)
    return response.data
  },

  /**
   * Deposit funds/assets
   */
  async deposit(request: DepositRequest): Promise<ApiResponse<CustomerAsset>> {
    const response = await api.post<ApiResponse<CustomerAsset>>('/assets/deposit', request)
    return response.data
  },

  /**
   * Withdraw funds/assets
   */
  async withdraw(request: WithdrawRequest): Promise<ApiResponse<CustomerAsset>> {
    const response = await api.post<ApiResponse<CustomerAsset>>('/assets/withdraw', request)
    return response.data
  },

  /**
   * Get asset statistics
   */
  async getAssetStats(customerId?: string): Promise<ApiResponse<AssetStats>> {
    const params = customerId ? `?customerId=${customerId}` : ''
    const response = await api.get<ApiResponse<AssetStats>>(`/assets/stats${params}`)
    return response.data
  },

  /**
   * Get all customers' TRY balances (ADMIN only)
   */
  async getAllTryBalances(): Promise<ApiResponse<CustomerAsset[]>> {
    const response = await api.get<ApiResponse<CustomerAsset[]>>('/assets/balances/try')
    return response.data
  }
}

export default assetService
