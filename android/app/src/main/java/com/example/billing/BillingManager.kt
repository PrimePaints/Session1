package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing for the one-time "Repeatless PRO" unlock.
 *
 * Product: non-consumable in-app product with ID [PRO_PRODUCT_ID] — must be
 * created in Play Console (Monetise → Products → In-app products) with the
 * same ID before purchases can work. Purchases only function on builds signed
 * and distributed through Play (internal testing is enough); on local debug
 * builds the query simply returns nothing.
 *
 * Entitlement model: Play is the source of truth. Every successful purchase
 * query pushes the result through [onEntitlementChanged], which the caller
 * persists (DataStore) so the unlock keeps working offline. Failed/offline
 * queries change nothing, so a cached entitlement is never revoked by a
 * network hiccup.
 */
class BillingManager(
    context: Context,
    private val onEntitlementChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val PRO_PRODUCT_ID = "pro_unlock"
        private const val TAG = "BillingManager"
    }

    data class BillingUiState(
        /** Localised, formatted price of the Pro unlock (e.g. "R89,99"), when known. */
        val proPrice: String? = null,
        /** True while Play reports the product owned. */
        val isPurchased: Boolean = false,
        /** One-shot user-facing message (pending payment, errors, restore result). */
        val message: String? = null,
        /** True when the Play connection is up and the product was found. */
        val canPurchase: Boolean = false
    )

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    refreshPurchases(notifyUser = false)
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() retries for us.
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    fun destroy() {
        billingClient.endConnection()
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsResult.productDetailsList.firstOrNull()
                productDetails = details
                _state.value = _state.value.copy(
                    proPrice = details?.oneTimePurchaseOfferDetails?.formattedPrice,
                    canPurchase = details != null
                )
                if (details == null) {
                    Log.w(TAG, "Product $PRO_PRODUCT_ID not found — create it in Play Console")
                }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * Re-checks owned purchases with Play. Acknowledges anything unacknowledged
     * and pushes the entitlement to the caller. Only a *successful* query may
     * change entitlement (offline never revokes).
     */
    fun refreshPurchases(notifyUser: Boolean) {
        if (!billingClient.isReady) {
            if (notifyUser) postMessage("Google Play is not available right now — try again shortly.")
            connect()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchases failed: ${result.debugMessage}")
                if (notifyUser) postMessage("Couldn't reach Google Play to restore purchases.")
                return@queryPurchasesAsync
            }
            val proPurchase = purchases.firstOrNull {
                it.products.contains(PRO_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            proPurchase?.let { acknowledgeIfNeeded(it) }

            val owned = proPurchase != null
            _state.value = _state.value.copy(isPurchased = owned)
            onEntitlementChanged(owned)
            if (notifyUser) {
                postMessage(if (owned) "Repeatless PRO restored ✓" else "No previous purchase found for this Google account.")
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            postMessage("Google Play is not available right now — try again shortly.")
            connect()
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            if (purchase.products.contains(PRO_PRODUCT_ID)) {
                                acknowledgeIfNeeded(purchase)
                                _state.value = _state.value.copy(isPurchased = true)
                                onEntitlementChanged(true)
                                postMessage("Repeatless PRO unlocked — thank you! 🎉")
                            }
                        }
                        Purchase.PurchaseState.PENDING -> {
                            postMessage("Payment pending — PRO will unlock automatically once it completes.")
                        }
                        else -> Unit
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // e.g. reinstall before the first query finished.
                refreshPurchases(notifyUser = false)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                Log.w(TAG, "Purchase failed: ${result.responseCode} ${result.debugMessage}")
                postMessage("Purchase didn't complete — you have not been charged.")
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                // Unacknowledged purchases are auto-refunded by Play after ~3 days;
                // the next refreshPurchases() retries.
                Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
            }
        }
    }

    private fun postMessage(message: String) {
        _state.value = _state.value.copy(message = message)
    }

    /** Call after showing [BillingUiState.message] so it doesn't re-show. */
    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
